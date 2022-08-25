package com.amaxilatis.metis.model;


import com.amaxilatis.metis.detector.client.DetectorApiClient;
import com.amaxilatis.metis.detector.client.dto.DataDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionsDTO;
import com.amaxilatis.metis.detector.client.dto.ImageDetectionResultDTO;
import com.amaxilatis.metis.util.CloudUtils;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.amaxilatis.metis.util.CloudUtils.BLACK_RGB;
import static com.amaxilatis.metis.util.CloudUtils.GRAY_RGB;
import static com.amaxilatis.metis.util.CloudUtils.WHITE_RGB;
import static com.amaxilatis.metis.util.CloudUtils.cleanupCloudsBasedOnNearby;
import static com.amaxilatis.metis.util.CloudUtils.cleanupCloudsBasedOnTiles;
import static com.amaxilatis.metis.util.ImageDataUtils.getImageTileDataFromCoordinates;
import static com.amaxilatis.metis.util.ImageDataUtils.isEmptyTile;
import static com.amaxilatis.metis.util.ImageDataUtils.isValidPixel;
import static org.apache.tika.mime.MimeTypes.OCTET_STREAM;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImagePack {
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
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
    private String cloudMaskDir;
    
    private DetectorApiClient detectorApiClient = new DetectorApiClient();
    
    /**
     * Creates an object that represents and Image file and acts as a helper for storing image properties across different tests.
     *
     * @param file         the file of the image.
     * @param histogramDir the directory where the histogram of the image needs to be stored.
     * @throws IOException
     */
    public ImagePack(final File file, final String histogramDir, final String cloudMaskDir) throws IOException {
        this.ioMetadata = null;
        this.image = null;
        this.metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, OCTET_STREAM);
        this.handler = new BodyContentHandler();
        this.inputStream = new FileInputStream(file);
        this.context = new ParseContext();
        this.file = file;
        this.histogramDir = histogramDir;
        this.cloudMaskDir = cloudMaskDir;
        
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
     * @throws IOException
     */
    public void detectClouds(boolean segmented) throws IOException {
        final BufferedImage jImage = ImageIO.read(file);
        //image information
        final int width = jImage.getWidth();
        final int height = jImage.getHeight();
        
        //mask images for cloud detection
        this.maskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        this.tensorflowMaskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        //        //do a first pass for cloud, histogram and contrast data
        //        parseImagePixels(width, height, jImage, false, false, true);
        //
        //        //check for single cloud pixels that are probably incorrect
        //        int removedPixels = cleanupCloudsBasedOnNearby(maskImage, width, height, 2);
        //        cloudPixels -= removedPixels;
        //
        //        removedPixels = cleanupCloudsBasedOnTiles(maskImage, width, height, 100, 3);
        //        cloudPixels -= removedPixels;
        //
        //        //write mask file to storage
        //        ImageIO.write(maskImage, "png", new File(cloudMaskDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-nai.mask.png"));
        
        final long start = System.currentTimeMillis();
        log.info(String.format("[%20s] starting TF cloud detection...", file.getName()));
        
        ImageDetectionResultDTO detectionResult = null;
        
        if (segmented) {
            detectionResult = detectCloudsInTilesOfImage(jImage, width, height);
        } else {
            detectionResult = detectCloudsInWholeImage();
        }
        
        //get results form the call
        validPixels = detectionResult.getPixels();
        cloudPixels = detectionResult.getCloudy();
        
        double percentage = validPixels / cloudPixels;
        log.info(String.format("[%20s] TF cloud detection result: %1.3f took: %d sec", file.getName(), percentage, (System.currentTimeMillis() - start) / 1000));
    }
    
    private ImageDetectionResultDTO detectCloudsInTilesOfImage(final BufferedImage image, final int width, final int height) throws IOException {
        double tfCheckedPixels = 0;
        double tfCloudPixels = 0;
        
        int components = 4;
        //paint the whole image gray - not yet processed
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                tensorflowMaskImage.setRGB(w, h, GRAY_RGB);
            }
        }
        //write mask file to storage
        updateMaskImage();
        
        int widthTiles = width / TILE_WIDTH;
        int heightTiles = height / TILE_HEIGHT;
        for (int w = 0; w < widthTiles; w++) {
            for (int h = 0; h < heightTiles; h++) {
                //all tiles in the TILE_WIDTH x TILE_HEIGHT grid
                tfCheckedPixels += (TILE_WIDTH * TILE_HEIGHT);
                tfCloudPixels += checkTileData(image, w * TILE_WIDTH, h * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, components);
            }
            
            //last tile in each column
            tfCheckedPixels += (TILE_WIDTH * (height - heightTiles * TILE_HEIGHT));
            tfCloudPixels += checkTileData(image, w * TILE_WIDTH, height - TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, components);
            
            //write mask file to storage
            updateMaskImage();
        }
        for (int h = 0; h < heightTiles; h++) {
            //last tile in each row
            tfCheckedPixels += ((width - widthTiles * TILE_WIDTH) * TILE_HEIGHT);
            tfCloudPixels += checkTileData(image, width - TILE_WIDTH, h * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, components);
        }
        
        //last tile in last row and column
        tfCheckedPixels += ((width - widthTiles * TILE_WIDTH) * (height - heightTiles * TILE_HEIGHT));
        tfCloudPixels += checkTileData(image, width - TILE_WIDTH, height - TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, components);
        
        int removedPixels = cleanupCloudsBasedOnNearby(tensorflowMaskImage, width, height, 2);
        tfCloudPixels -= removedPixels;
        
        removedPixels = cleanupCloudsBasedOnTiles(tensorflowMaskImage, width, height, 100, 3);
        tfCloudPixels -= removedPixels;
        
        //write mask file to storage
        updateMaskImage();
        
        return new ImageDetectionResultDTO(null, null, (int) tfCheckedPixels, (int) tfCloudPixels, tfCloudPixels / tfCheckedPixels);
    }
    
    private void updateMaskImage() throws IOException {
        ImageIO.write(tensorflowMaskImage, "png", new File(FileNameUtils.getImageCloudCoverMaskFilename(cloudMaskDir, this.file.getParentFile().getName(), this.file.getName())));
    }
    
    private int checkTileData(final BufferedImage image, final int startWidth, final int startHeight, final int tileWidth, final int tileHeight, final int components) {
        int thisTileCloudPixels = 0;
        int[] dnValues = getImageTileDataFromCoordinates(image, tileWidth, tileHeight, components, startWidth, startHeight);
        if (isEmptyTile(dnValues)) {
            log.trace(String.format("[%20s] tile_l:[%04d,%04d] skipping...", file.getName(), startWidth, startHeight));
            for (int j = 0; j < TILE_WIDTH; j++) {
                for (int k = 0; k < TILE_HEIGHT; k++) {
                    tensorflowMaskImage.setRGB(startWidth + k, startHeight + j, BLACK_RGB);
                }
            }
        } else {
            log.trace(String.format("[%20s] tile_l:[%04d,%04d] detecting...", file.getName(), startWidth, startHeight));
            
            DetectionsDTO detectionsDTO;
            do {
                detectionsDTO = detectorApiClient.postData(DataDTO.builder().w(startWidth).h(startHeight).data(dnValues).build());
            } while (detectionsDTO == null);
            
            for (int j = 0; j < TILE_WIDTH; j++) {
                for (int k = 0; k < TILE_HEIGHT; k++) {
                    thisTileCloudPixels += detectionsDTO.getPredictions()[j][k];
                    int mx = startWidth + k;
                    int my = startHeight + j;
                    tensorflowMaskImage.setRGB(mx, my, detectionsDTO.getPredictions()[j][k] == 1 ? WHITE_RGB : BLACK_RGB);
                }
            }
        }
        return thisTileCloudPixels;
    }
    
    private ImageDetectionResultDTO detectCloudsInWholeImage() {
        //make a call to the cloud detection server for the whole image
        return detectorApiClient.checkImageFile(file.getAbsolutePath(), new File(FileNameUtils.getImageCloudCoverMaskFilename(cloudMaskDir, this.file.getParentFile().getName(), this.file.getName())).getAbsolutePath());
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
    
    
    private void updateHistogram(final Color color) {
        histogram.addValues(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }
    
    private void updateContrastData(final Color color) {
        final double brightness = ColorUtils.getBrightness(color);
        dnValuesStatistics.addValue(brightness);
    }
    
    private void updateCloudData(final int x, final int y, final Color color) {
        final float[] hsv = new float[3];
        
        double colorRed = ((1 - color.getAlpha() / 255.0) * color.getRed() / 255.0) + (color.getAlpha() / 255.0 * color.getRed() / 255.0);
        double colorGreen = ((1 - color.getAlpha() / 255.0) * color.getGreen() / 255.0) + (color.getAlpha() / 255.0 * color.getGreen() / 255.0);
        double colorBlue = ((1 - color.getAlpha() / 255.0) * color.getBlue() / 255.0) + (color.getAlpha() / 255.0 * color.getBlue() / 255.0);
        
        //Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
        Color.RGBtoHSB((int) (colorRed * 255), (int) (colorGreen * 255), (int) (colorBlue * 255), hsv);
        final boolean isCloud = CloudUtils.isCloud(hsv[0] * 255, hsv[1] * 255, hsv[2] * 255);
        //log.info("h: {} s: {} v: {} | cloudProbability:{}", hsv[0] * 255, hsv[1] * 255, hsv[2] * 255, cloudProbability);
        if (isCloud) {
            cloudPixels++;
        }
        maskImage.setRGB(x, y, isCloud ? WHITE_RGB : BLACK_RGB);
        validPixels++;
    }
}
