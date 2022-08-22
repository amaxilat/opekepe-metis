package com.amaxilatis.metis.model;


import com.amaxilatis.metis.detector.client.DetectorApiClient;
import com.amaxilatis.metis.detector.client.dto.DataDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionsDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionsListDTO;
import com.amaxilatis.metis.detector.client.dto.ImageDetectionResultDTO;
import com.amaxilatis.metis.util.ColorUtils;
import com.amaxilatis.metis.util.FileNameUtils;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.image.TiffParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.amaxilatis.metis.model.CloudUtils.BLACK_RGB;
import static com.amaxilatis.metis.model.CloudUtils.WHITE_RGB;
import static org.apache.tika.mime.MimeTypes.OCTET_STREAM;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImagePack {
    private com.drew.metadata.Metadata ioMetadata;
    private BufferedImage image;
    @Getter
    private final Metadata metadata;
    private final File file;
    @Getter
    private final BodyContentHandler handler;
    @Getter
    private final FileInputStream inputStream;
    @Getter
    private final ParseContext context;
    @Getter
    private HistogramsHelper histogram;
    @Getter
    private boolean loaded;
    private boolean histogramLoaded;
    @Getter
    private SummaryStatistics dnValuesStatistics;
    @Getter
    private double validPixels = 0;
    @Getter
    private double cloudPixels = 0;
    @Getter
    private BufferedImage maskImage;
    private BufferedImage tensorflowMaskImage;
    private String histogramDir;
    
    private DetectorApiClient detectorApiClient = new DetectorApiClient();
    
    /**
     * Creates an object that represents and Image file and acts as a helper for storing image properties across different tests.
     *
     * @param file         the file of the image.
     * @param histogramDir the directory where the histogram of the image needs to be stored.
     * @throws IOException
     */
    public ImagePack(final File file, final String histogramDir) throws IOException {
        this.ioMetadata = null;
        this.image = null;
        this.metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, OCTET_STREAM);
        this.handler = new BodyContentHandler();
        this.inputStream = new FileInputStream(file);
        this.context = new ParseContext();
        this.file = file;
        this.histogramDir = histogramDir;
        
        this.loaded = false;
        this.histogramLoaded = false;
    }
    
    public com.drew.metadata.Metadata getIoMetadata() throws ImageProcessingException, IOException {
        if (this.ioMetadata == null) {
            this.ioMetadata = ImageMetadataReader.readMetadata(file);
        }
        return ioMetadata;
    }
    
    public BufferedImage getImage() throws IOException {
        if (this.image == null) {
            this.image = ImageIO.read(file);
        }
        return image;
    }
    
    /**
     * Load the image using apache tika
     *
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public void loadImage() throws IOException, TikaException, SAXException {
        if (!loaded) {
            long start = System.currentTimeMillis();
            final TiffParser JpegParser = new TiffParser();
            JpegParser.parse(this.getInputStream(), this.getHandler(), this.getMetadata(), this.getContext());
            log.info("[{}] jpegParser took {}ms", file.getName(), (System.currentTimeMillis() - start));
            this.loaded = true;
        }
    }
    
    /**
     * Parse the image and generate its histogram
     *
     * @throws IOException
     */
    public void loadHistogram() throws IOException {
        if (!histogramLoaded) {
            
            final BufferedImage jImage = ImageIO.read(file);
            //histogram
            this.histogram = new HistogramsHelper();
            //image information
            final int width = jImage.getWidth();
            final int height = jImage.getHeight();
            // pixel value statistics
            this.dnValuesStatistics = new SummaryStatistics();
            
            //do a first pass for cloud, histogram and contrast data
            parseImagePixels(width, height, jImage, true, true, false);
            
            this.histogramLoaded = true;
        }
    }
    
    /**
     * Parse the image and generate its histogram
     *
     * @param debugCloudDetection flag use to enable generation of intermediate cloud detection masks.
     * @throws IOException
     */
    public void detectClouds(final boolean debugCloudDetection, boolean segmented) throws IOException {
        final BufferedImage jImage = ImageIO.read(file);
        //image information
        final int width = jImage.getWidth();
        final int height = jImage.getHeight();
        
        //mask images for cloud detection
        this.maskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        this.tensorflowMaskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        //do a first pass for cloud, histogram and contrast data
        parseImagePixels(width, height, jImage, false, false, true);
        
        if (debugCloudDetection) {
            //debug
            ImageIO.write(maskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-nai-a.mask.png"));
        }
        
        //check for single cloud pixels that are probably incorrect
        int removedPixels = cleanupCloudsBasedOnNearby(maskImage, width, height, 2);
        cloudPixels -= removedPixels;
        if (debugCloudDetection) {
            //debug
            ImageIO.write(maskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-nai-b.mask.png"));
        }
        
        removedPixels = cleanupCloudsBasedOnTiles(maskImage, width, height, 100, 3);
        cloudPixels -= removedPixels;
        if (debugCloudDetection) {
            //debug
            ImageIO.write(maskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-nai-c.mask.png"));
        }
        
        //write mask file to storage
        ImageIO.write(maskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-nai.mask.png"));
        
        
        int c_checkedPixels = 0;
        int c_cloudPixels = 0;
        
        final long start = System.currentTimeMillis();
        log.info(String.format("[%20s] starting TF cloud detection...", file.getName()));
        
        ImageDetectionResultDTO detectionResult = null;
        
        if (segmented) {
            detectionResult = detectCloudsInTilesOfImage(jImage, width, height, debugCloudDetection);
            
            
        } else {
            detectionResult = detectCloudsInWholeImage();
        }
        
        //get results form the call
        c_checkedPixels = detectionResult.getPixels();
        c_cloudPixels = detectionResult.getCloudy();
        
        validPixels = c_checkedPixels;
        cloudPixels = c_cloudPixels;
        
        double percentage = validPixels / cloudPixels;
        log.info(String.format("[%20s] TF cloud detection result: %1.3f took: %d sec", file.getName(), percentage, (System.currentTimeMillis() - start) / 1000));
    }
    
    private ImageDetectionResultDTO detectCloudsInTilesOfImage(final BufferedImage image, final int width, final int height, final boolean debugCloudDetection) throws IOException {
        double c_checkedPixels = 0;
        double c_cloudPixels = 0;
        
        int components = 4;
        int widthTiles = width / 256;
        int heightTiles = height / 256;
        for (int w = 0; w < widthTiles; w++) {
            for (int h = 0; h < heightTiles; h++) {
                List<DataDTO> tiles = new ArrayList<>();
                
                final int size = 256 * 256 * components;
                int[] dnValues = new int[size];
                image.getData().getPixels(w * 256, h * 256, 256, 256, dnValues);
                c_checkedPixels += (256 * 256);
                if (isEmptyTile(dnValues)) {
                    log.trace(String.format("[%20s] tile:[%02d,%02d] skipping...", file.getName(), w, h));
                    for (int j = 0; j < 256; j++) {
                        for (int k = 0; k < 256; k++) {
                            tensorflowMaskImage.setRGB(w * 256 + k, h * 256 + j, BLACK_RGB);
                        }
                    }
                } else {
                    log.trace(String.format("[%20s] tile:[%02d,%02d] detecting...", file.getName(), w, h));
                    tiles.add(DataDTO.builder().w(w).h(h).data(dnValues).build());
                }
                
                if (!tiles.isEmpty()) {
                    DetectionsListDTO responseList;
                    do {
                        responseList = detectorApiClient.postData(tiles);
                    } while (responseList == null);
                    for (DetectionsDTO response : responseList.getTiles()) {
                        for (int j = 0; j < 256; j++) {
                            for (int k = 0; k < 256; k++) {
                                c_cloudPixels += response.getPredictions()[j][k];
                                int mx = response.getW() * 256 + k;
                                int my = response.getH() * 256 + j;
                                tensorflowMaskImage.setRGB(mx, my, response.getPredictions()[j][k] == 1 ? WHITE_RGB : BLACK_RGB);
                            }
                        }
                    }
                }
            }
        }
        
        
        if (debugCloudDetection) {
            //debug
            ImageIO.write(tensorflowMaskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-tf-a.mask.png"));
        }
        
        cleanupCloudsBasedOnNearby(tensorflowMaskImage, width, height, 2);
        if (debugCloudDetection) {
            //debug
            ImageIO.write(tensorflowMaskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-tf-b.mask.png"));
        }
        
        cleanupCloudsBasedOnTiles(tensorflowMaskImage, width, height, 100, 3);
        if (debugCloudDetection) {
            //debug
            ImageIO.write(tensorflowMaskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-tf-c.mask.png"));
        }
        
        //write mask file to storage
        ImageIO.write(tensorflowMaskImage, "png", new File(FileNameUtils.getImageCloudCoverMaskFilename(histogramDir, this.file.getParentFile().getName(), this.file.getName())));
        
        return new ImageDetectionResultDTO(null, null, (int) c_checkedPixels, (int) c_cloudPixels, c_cloudPixels / c_checkedPixels);
    }
    
    private ImageDetectionResultDTO detectCloudsInWholeImage() {
        //make a call to the cloud detection server for the whole image
        return detectorApiClient.checkImageFile(file.getAbsolutePath(), new File(FileNameUtils.getImageCloudCoverMaskFilename(histogramDir, this.file.getParentFile().getName(), this.file.getName())).getAbsolutePath());
    }
    
    /**
     * Checks if the tile is empty, with full black or full white pixels.
     *
     * @param pixelValues the pixel values
     * @return true if all the pixels are empty and false if at least one is not.
     */
    private boolean isEmptyTile(final int[] pixelValues) {
        for (int i = 0; i < pixelValues.length; i += 4) {
            if (isValidPixel(pixelValues[i], pixelValues[i + 1], pixelValues[i + 2], pixelValues[i + 3])) {
                return false;
            }
        }
        return true;
    }
    
    private void parseImagePixels(final int width, final int height, final BufferedImage jImage, final boolean updateHistogram, final boolean updateContrast, final boolean detectClouds) {
        int heightStep = height / 10;
        int heightStart = 0;
        int currentStep = heightStep;
        do {
            if (heightStart + currentStep > height) {
                currentStep = height - heightStart;
            }
            if (currentStep == 0) {
                break;
            }
            final int size = width * heightStep;
            int dnValues[] = new int[size];
            dnValues = jImage.getRGB(0, heightStart, width, currentStep, dnValues, 0, width);
            for (int i = 0; i < size; i++) {
                final Color color = new Color(dnValues[i], true);
                final int x = (i) % width;
                final int y = (i) / width + heightStart;
                if (isValidPixel(color)) {
                    if (detectClouds) {
                        //update the cloud data for check 4
                        updateCloudData(x, y, color);
                    }
                    if (updateHistogram) {
                        //update histogram for check 5,6
                        updateHistogram(color);
                    }
                    if (updateContrast) {
                        //update the cloud data for check 7
                        updateContrastData(color);
                    }
                }
            }
            heightStart += heightStep;
        } while (heightStart <= height);
    }
    
    /**
     * Cleans up the image mask based on the percentage of cloud pixels found inside each tile of the provided size.
     *
     * @param image               the object containing the image mask
     * @param width               the width of the image
     * @param height              the height of the image
     * @param tileSize            the size of the tile
     * @param percentageThreshold the top threshold to consider a cloudy tile invalid (0-100)
     * @return the number of the removed cloud pixels.
     */
    private int cleanupCloudsBasedOnTiles(final BufferedImage image, final int width, final int height, final int tileSize, final int percentageThreshold) {
        int removedPixels = 0;
        final int widthTiles = width / tileSize;
        final int heightTiles = height / tileSize;
        final int tileArea = tileSize * tileSize;
        final int[] maskValues = new int[tileArea];
        for (int w = 0; w < widthTiles; w++) {
            for (int h = 0; h < heightTiles; h++) {
                image.getRGB(w * tileSize, h * tileSize, tileSize, tileSize, maskValues, 0, tileSize);
                double count = 0;
                for (int maskValue : maskValues) {
                    if (maskValue == WHITE_RGB) {
                        count++;
                    }
                }
                final double percentage = (count / (tileArea)) * tileSize;
                if (count > 0 && percentage <= percentageThreshold) {
                    //if less than 1% of the tile are clouds, then probably no cloud in the tile
                    log.trace("[{}] count: {}, total: {}, p: {}", file.getName(), count, tileArea, percentage);
                    for (int i = 0; i < tileSize; i++) {
                        for (int j = 0; j < tileSize; j++) {
                            if (image.getRGB(w * tileSize + i, h * tileSize + j) == WHITE_RGB) {
                                image.setRGB(w * tileSize + i, h * tileSize + j, BLACK_RGB);
                                removedPixels++;
                            }
                        }
                    }
                }
            }
        }
        return removedPixels;
    }
    
    /**
     * Cleans up the image mask based on the number of nearby pixels belonging to a cloud, used to remove rogue cloud pixels that are probably false positives.
     *
     * @param image     the object containing the image mask
     * @param width     the width of the image
     * @param height    the height of the image
     * @param threshold the number of nearby pixels needed to consider a pixel a valid cloudy pixel
     * @return the number of the removed cloud pixels.
     */
    private int cleanupCloudsBasedOnNearby(final BufferedImage image, final int width, final int height, final int threshold) {
        int removedPixels = 0;
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (image.getRGB(x, y) == WHITE_RGB) {
                    if (!isAnyNearby(image, x, y, WHITE_RGB, threshold)) {
                        image.setRGB(x, y, BLACK_RGB);
                        removedPixels++;
                    }
                }
            }
        }
        return removedPixels;
    }
    
    /**
     * Checks if the nearby pixels of the defined x,y pixel are cloudy or not and the number of the cloud pixels with regard to a given threshold.
     *
     * @param image         the object containing the image mask
     * @param x             the x coordinate of the pixel to check
     * @param y             the y coordinate of the pixel to check
     * @param specificColor the color to check for
     * @param threshold     the number of nearby pixels needed to consider a pixel a valid cloudy pixel
     * @return true if the pixel is considered valid, false if it was a false positive.
     */
    private boolean isAnyNearby(final BufferedImage image, final int x, final int y, final int specificColor, final int threshold) {
        int nearby = 0;
        nearby += image.getRGB(x - 1, y) == specificColor ? 1 : 0;
        nearby += image.getRGB(x + 1, y) == specificColor ? 1 : 0;
        nearby += image.getRGB(x, y - 1) == specificColor ? 1 : 0;
        nearby += image.getRGB(x, y + 1) == specificColor ? 1 : 0;
        nearby += image.getRGB(x - 1, y - 1) == specificColor ? 1 : 0;
        nearby += image.getRGB(x - 1, y + 1) == specificColor ? 1 : 0;
        nearby += image.getRGB(x + 1, y - 1) == specificColor ? 1 : 0;
        nearby += image.getRGB(x + 1, y + 1) == specificColor ? 1 : 0;
        return nearby > threshold;
    }
    
    /**
     * Checks if the pixel's value is valid (non-white and non-black).
     *
     * @param pixelColor the pixel's color
     * @return true if the pixel is non-white and non-black, false else.
     */
    private boolean isValidPixel(final Color pixelColor) {
        return isValidPixel(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue(), pixelColor.getAlpha());
    }
    
    /**
     * Checks if the pixel's value is valid (non-white and non-black).
     *
     * @param r   the red component of the pixel
     * @param g   the green component of the pixel
     * @param b   the blue component of the pixel
     * @param nir the nir comomponent of the pixel
     * @return true if the pixel is non-white and non-black, false else.
     */
    private boolean isValidPixel(final int r, final int g, final int b, final int nir) {
        return (255 != r || 255 != g || 255 != b || 255 != nir) && (0 != r || 0 != g || 0 != b || 0 != nir);
    }
    
    private void updateHistogram(final Color color) {
        histogram.addValues(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }
    
    private void updateContrastData(final Color color) {
        final double brightness = ColorUtils.getBrightness(color);
        dnValuesStatistics.addValue(brightness);
    }
    
    private void updateCloudData(final int x, final int y, final Color color) {
        final float[] hsv = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
        final boolean isCloud = CloudUtils.isCloud(hsv[0] * 255, hsv[1] * 255, hsv[2] * 255);
        //log.info("h: {} s: {} v: {} | cloudProbability:{}", hsv[0] * 255, hsv[1] * 255, hsv[2] * 255, cloudProbability);
        if (isCloud) {
            cloudPixels++;
        }
        maskImage.setRGB(x, y, isCloud ? WHITE_RGB : BLACK_RGB);
        validPixels++;
    }
}
