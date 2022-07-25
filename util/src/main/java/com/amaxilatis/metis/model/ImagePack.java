package com.amaxilatis.metis.model;


import com.amaxilatis.metis.cdclient.ApiClient;
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
    private int validPixels = 0;
    @Getter
    private int cloudPixels = 0;
    @Getter
    private BufferedImage maskImage;
    private BufferedImage tensorflowMaskImage;
    private String histogramDir;
    
    private ApiClient apiClient = new ApiClient();
    
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
            
            //mask images for cloud detection
            this.maskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            this.tensorflowMaskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            
            //do a first pass for cloud, histogram and contrast data
            parseImagePixels(width, height, jImage);
            
            //debug
            ImageIO.write(maskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "a.mask.png"));
            
            //check for single cloud pixels that are probably incorrect
            cleanupCloudsBasedOnNearby(width, height, 2);
            //debug
            ImageIO.write(maskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "b.mask.png"));
            
            cleanupCloudsBasedOnTiles(width, height, 100, 3);
            //debug
            ImageIO.write(maskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "c.mask.png"));
            
            //write mask file to storage
            ImageIO.write(maskImage, "png", new File(FileNameUtils.getImageCloudCoverMaskFilename(histogramDir, this.file.getParentFile().getName(), this.file.getName())));
            
            //            int c_checkedPixels = 0;
            //            int c_cloudPixels = 0;
            //
            //            log.error("{} starting cloud detection", file.getName());
            //
            //            heightStep = 256;
            //            widthTiles = width / 256;
            //            heightTiles = height / 256;
            //            for (int w = 0; w < widthTiles; w++) {
            //                for (int h = 0; h < heightTiles; h++) {
            //                    final int size = 256 * 256 * components;
            //                    final int sizeRGB = 256 * 256 * 3;
            //                    int[] dnValues = new int[size];
            //                    //                    int[] dnValuesRGB = new int[sizeRGB];
            //                    log.error("{} {} {}", file.getName(), w, h);
            //                    jImage.getData().getPixels(w * 256, h * 256, 256, 256, dnValues);
            //                    //                    for (int i = 0; i < 256 * 256; i++) {
            //                    //                        dnValuesRGB[i * 3] = dnValues[i * 4];
            //                    //                        dnValuesRGB[i * 3 + 1] = dnValues[i * 4 + 1];
            //                    //                        dnValuesRGB[i * 3 + 2] = dnValues[i * 4 + 2];
            //                    //                    }
            //
            //                    log.error("{} {} {}", file.getName(), w, h);
            //                    DetectionsDTO response;
            //                    do {
            //                        response = apiClient.postData(DataDTO.builder().data(dnValues).build());
            //                    } while (response == null);
            //                    c_checkedPixels += (256 * 256);
            //                    for (int j = 0; j < 256; j++) {
            //
            //                        for (int k = 0; k < 256; k++) {
            //                            c_cloudPixels += response.getPredictions()[j][k];
            //                            int mx = w * 256 + k;
            //                            int my = h * 256 + j;
            //                            tensorflowMaskImage.setRGB(mx, my, response.getPredictions()[j][k] == 1 ? WHITE_RGB : BLACK_RGB);
            //                        }
            //                    }
            //                }
            //            }
            //            log.info("cloudPixels {} / {} c_cloudPixels {} / {}", cloudPixels, validPixels, c_cloudPixels, c_checkedPixels);
            //
            //            //write mask file to storage
            //            ImageIO.write(tensorflowMaskImage, "png", new File(histogramDir + "/" + this.file.getParentFile().getName(), this.file.getName() + "-1.mask.png"));
            
            this.histogramLoaded = true;
        }
    }
    
    private void parseImagePixels(final int width, final int height, final BufferedImage jImage) {
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
                final int r = color.getRed();
                final int g = color.getGreen();
                final int b = color.getBlue();
                final int nir = color.getAlpha();
                final int x = (i) % width;
                final int y = (i) / width + heightStart;
                if (isValidPixel(r, g, b, nir)) {
                    //update the cloud data for check 4
                    updateCloudData(x, y, r, g, b);
                    //update histogram for check 5,6
                    updateHistogram(r, g, b, nir);
                    //update the cloud data for check 7
                    updateContrastData(r, g, b);
                }
            }
            heightStart += heightStep;
        } while (heightStart <= height);
    }
    
    private void cleanupCloudsBasedOnTiles(int width, int height, int tileSize, int percentageThreshold) {
        final int widthTiles = width / tileSize;
        final int heightTiles = height / tileSize;
        final int tileArea = tileSize * tileSize;
        final int[] maskValues = new int[tileArea];
        for (int w = 0; w < widthTiles; w++) {
            for (int h = 0; h < heightTiles; h++) {
                maskImage.getRGB(w * tileSize, h * tileSize, tileSize, tileSize, maskValues, 0, tileSize);
                double count = 0;
                for (int maskValue : maskValues) {
                    if (maskValue == WHITE_RGB) {
                        count++;
                    }
                }
                final double percentage = (count / (tileArea)) * tileSize;
                if (count > 0 && percentage <= percentageThreshold) {
                    //if less than 1% of the tile are clouds, then probably no cloud in the tile
                    log.info("[{}] count: {}, total: {}, p: {}", file.getName(), count, tileArea, percentage);
                    for (int i = 0; i < tileSize; i++) {
                        for (int j = 0; j < tileSize; j++) {
                            if (maskImage.getRGB(w * tileSize + i, h * tileSize + j) == WHITE_RGB) {
                                maskImage.setRGB(w * tileSize + i, h * tileSize + j, BLACK_RGB);
                                cloudPixels--;
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void cleanupCloudsBasedOnNearby(final int width, final int height, final int threshold) {
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (maskImage.getRGB(x, y) == WHITE_RGB) {
                    if (!isAnyNearby(maskImage, x, y, WHITE_RGB, threshold)) {
                        maskImage.setRGB(x, y, BLACK_RGB);
                        cloudPixels--;
                    }
                }
            }
        }
    }
    
    private boolean isAnyNearby(final BufferedImage maskImage, final int x, final int y, final int specificColor, final int threshold) {
        int nearby = 0;
        nearby += maskImage.getRGB(x - 1, y) == specificColor ? 1 : 0;
        nearby += maskImage.getRGB(x + 1, y) == specificColor ? 1 : 0;
        nearby += maskImage.getRGB(x, y - 1) == specificColor ? 1 : 0;
        nearby += maskImage.getRGB(x, y + 1) == specificColor ? 1 : 0;
        nearby += maskImage.getRGB(x - 1, y - 1) == specificColor ? 1 : 0;
        nearby += maskImage.getRGB(x - 1, y + 1) == specificColor ? 1 : 0;
        nearby += maskImage.getRGB(x + 1, y - 1) == specificColor ? 1 : 0;
        nearby += maskImage.getRGB(x + 1, y + 1) == specificColor ? 1 : 0;
        return nearby > threshold;
    }
    
    private boolean isValidPixel(final int r, final int g, final int b, final int nir) {
        return (255 != r || 255 != g || 255 != b || 255 != nir) && (0 != r || 0 != g || 0 != b || 0 != nir);
    }
    
    private void updateHistogram(final int r, final int g, final int b, final int nir) {
        histogram.addValues(r, g, b, nir);
    }
    
    private void updateContrastData(final int r, final int g, final int b) {
        final double brightness = ColorUtils.getBrightness(r, g, b);
        dnValuesStatistics.addValue(brightness);
    }
    
    private void updateCloudData(final int x, final int y, final int r, final int g, final int b) {
        final float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);
        final boolean isCloud = CloudUtils.isCloud(hsv[0] * 255, hsv[1] * 255, hsv[2] * 255);
        //log.info("h: {} s: {} v: {} | cloudProbability:{}", hsv[0] * 255, hsv[1] * 255, hsv[2] * 255, cloudProbability);
        if (isCloud) {
            cloudPixels++;
        }
        maskImage.setRGB(x, y, isCloud ? WHITE_RGB : BLACK_RGB);
        validPixels++;
    }
}
