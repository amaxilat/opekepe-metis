package com.amaxilatis.metis.model;


import com.amaxilatis.metis.detector.client.DetectorApiClient;
import com.amaxilatis.metis.detector.client.dto.DataDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionsDTO;
import com.amaxilatis.metis.detector.client.dto.ImageDetectionResultDTO;
import com.amaxilatis.metis.util.CloudUtils;
import com.amaxilatis.metis.util.ColorUtils;
import com.amaxilatis.metis.util.CompressionUtils;
import com.amaxilatis.metis.util.FileNameUtils;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi;
import com.github.jaiimageio.impl.plugins.tiff.TIFFImageWriterSpi;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.image.TiffParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.amaxilatis.metis.util.CloudUtils.BLACK_RGB;
import static com.amaxilatis.metis.util.CloudUtils.GRAY_RGB;
import static com.amaxilatis.metis.util.CloudUtils.WHITE_RGB;
import static com.amaxilatis.metis.util.CloudUtils.cleanupCloudsBasedOnNearby;
import static com.amaxilatis.metis.util.CloudUtils.cleanupCloudsBasedOnTiles;
import static com.amaxilatis.metis.util.ImageDataUtils.getImageTileDataFromCoordinates;
import static com.amaxilatis.metis.util.ImageDataUtils.isEmptyTile;
import static com.amaxilatis.metis.util.ImageDataUtils.isValidPixel;
import static com.drew.metadata.exif.ExifDirectoryBase.TAG_COMPRESSION;
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
    private final File uncompressedImageFile;
    private final String name;
    private final String parentDirName;
    private final File dataFile;
    private final BodyContentHandler handler;
    private final FileInputStream inputStream;
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
    @Getter
    private final int compressionExifValue;
    private BufferedImage tensorflowMaskImage;
    private final String cloudMaskDir;
    private final int workers;
    
    private final DetectorApiClient detectorApiClient = new DetectorApiClient();
    private int componentSize;
    @Getter
    private int componentMaxValue;
    
    /**
     * Creates an object that represents and Image file and acts as a helper for storing image properties across different tests.
     *
     * @param file                 the file of the image.
     * @param cloudMaskDir         location where cloudMasks are stored
     * @param uncompressedLocation location where uncompressed images are stored
     * @param workers              concurrency used when calculating cloud coverage
     * @throws IOException
     */
    public ImagePack(final File file, final String cloudMaskDir, final String uncompressedLocation, final Integer workers) throws IOException, ImageProcessingException {
        this.workers = workers;
        this.cloudMaskDir = cloudMaskDir;
        this.name = file.getName();
        
        this.ioMetadata = ImageMetadataReader.readMetadata(file);
        this.parentDirName = file.getParentFile().getName();
        
        compressionExifValue = Integer.parseInt(ioMetadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(TAG_COMPRESSION));
        if (CompressionUtils.isCompressed(compressionExifValue) && CompressionUtils.isLossless(compressionExifValue)) {
            this.uncompressedImageFile = new File(FileNameUtils.getImageUncompressedFilename(uncompressedLocation, file.getParentFile().getName(), file.getName()));
            log.info("[{}] is compressed and need to be uncompressed as {}", file.getName(), uncompressedImageFile);
            
            if (!uncompressedImageFile.getParentFile().exists()) {
                uncompressedImageFile.getParentFile().mkdir();
            }
            
            final TIFFImageReaderSpi readerSpi = new TIFFImageReaderSpi();
            final ImageReader imageReader = readerSpi.createReaderInstance();
            final ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
            imageReader.setInput(imageInputStream);
            
            final TIFFImageWriterSpi writerSpi = new TIFFImageWriterSpi();
            final ImageWriter imageWriter = writerSpi.createWriterInstance();
            final ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
            imageWriteParam.setCompressionMode(ImageWriteParam.MODE_DISABLED);
            //bufferFile is created in the constructor
            final ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(uncompressedImageFile);
            imageWriter.setOutput(imageOutputStream);
            
            //Now read the bitmap
            final BufferedImage bufferedImage = imageReader.read(0);
            IIOImage iIOImage = new IIOImage(bufferedImage, null, null);
            //and write it
            imageWriter.write(null, iIOImage, imageWriteParam);
            imageWriter.dispose();
            imageOutputStream.flush();
            imageOutputStream.close();
            this.dataFile = uncompressedImageFile;
        } else {
            this.dataFile = file;
            this.uncompressedImageFile = null;
        }
        this.image = null;
        this.metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, OCTET_STREAM);
        this.handler = new BodyContentHandler();
        this.inputStream = new FileInputStream(dataFile);
        this.context = new ParseContext();
        
        this.loaded = false;
        this.histogramLoaded = false;
    }
    
    public com.drew.metadata.Metadata getIoMetadata() {
        return ioMetadata;
    }
    
    public BufferedImage getImage() throws IOException {
        if (this.image == null) {
            this.image = ImageIO.read(dataFile);
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
    public void loadImage() throws IOException, TikaException, SAXException, ImageProcessingException {
        if (!loaded) {
            long start = System.currentTimeMillis();
            final TiffParser imageParser = new TiffParser();
            imageParser.parse(inputStream, handler, metadata, context);
            log.info("[{}] jpegParser took {}ms", name, (System.currentTimeMillis() - start));
            getImage();
            this.loaded = true;
        }
    }
    
    /**
     * Parse the image and generate its histogram
     *
     * @throws IOException
     */
    public void loadHistogram() throws IOException, ImageProcessingException, TikaException, SAXException {
        loadImage();
        if (!histogramLoaded) {
            
            final BufferedImage jImage = ImageIO.read(dataFile);
    
            componentSize = image.getColorModel().getPixelSize() / image.getColorModel().getNumComponents();
            componentMaxValue = (int) Math.pow(2, componentSize);
            
            //histogram
            //values added to histogram are always scaled to 0-256
            this.histogram = new HistogramsHelper(256);
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
        final BufferedImage jImage = ImageIO.read(dataFile);
        //image information
        final int width = jImage.getWidth();
        final int height = jImage.getHeight();
        
        //mask images for cloud detection
        this.maskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        this.tensorflowMaskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        final long start = System.currentTimeMillis();
        log.info(String.format("[%20s] starting TF cloud detection...", name));
        
        ImageDetectionResultDTO detectionResult;
        
        if (segmented) {
            detectionResult = detectCloudsInTilesOfImage(jImage, width, height);
        } else {
            detectionResult = detectCloudsInWholeImage();
        }
        
        //get results form the call
        validPixels = detectionResult.getPixels();
        cloudPixels = detectionResult.getCloudy();
        
        double percentage = validPixels / cloudPixels;
        log.info(String.format("[%20s] TF cloud detection result: %1.3f took: %d sec", name, percentage, (System.currentTimeMillis() - start) / 1000));
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
        
        final ExecutorService executorService = Executors.newFixedThreadPool(workers);
        final ArrayList<Future<Integer>> futures = new ArrayList<>();
        
        int widthTiles = width / TILE_WIDTH;
        int heightTiles = height / TILE_HEIGHT;
        
        for (int w = 0; w < widthTiles; w++) {
            for (int h = 0; h < heightTiles; h++) {
                
                tfCheckedPixels += (TILE_WIDTH * TILE_HEIGHT);
                int finalW = w;
                int finalH = h;
                futures.add(executorService.submit(() -> {
                    //all tiles in the TILE_WIDTH x TILE_HEIGHT grid
                    return checkTileData(image, finalW * TILE_WIDTH, finalH * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, components);
                }));
            }
            
            //last tile in each column
            tfCheckedPixels += (TILE_WIDTH * (height - heightTiles * TILE_HEIGHT));
            int finalW1 = w;
            futures.add(executorService.submit(() -> {
                //all tiles in the TILE_WIDTH x TILE_HEIGHT grid
                return checkTileData(image, finalW1 * TILE_WIDTH, height - TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, components);
            }));
        }
        for (int h = 0; h < heightTiles; h++) {
            //last tile in each row
            tfCheckedPixels += ((width - widthTiles * TILE_WIDTH) * TILE_HEIGHT);
            int finalH = h;
            futures.add(executorService.submit(() -> {
                //all tiles in the TILE_WIDTH x TILE_HEIGHT grid
                return checkTileData(image, width - TILE_WIDTH, finalH * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, components);
            }));
        }
        
        //last tile in last row and column
        tfCheckedPixels += ((width - widthTiles * TILE_WIDTH) * (height - heightTiles * TILE_HEIGHT));
        futures.add(executorService.submit(() -> {
            //all tiles in the TILE_WIDTH x TILE_HEIGHT grid
            return checkTileData(image, width - TILE_WIDTH, height - TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, components);
        }));
        
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        for (final Future<Integer> future : futures) {
            // Process each `future` object. Get the result of each task's calculation. Sum total.
            if (future.isCancelled()) {
                log.error("Oops, this future is canceled.");
            } else if (future.isDone()) {
                
                try {
                    tfCloudPixels += future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }
            } else {
                log.error("future is not cancelled or done???");
            }
        }
        
        int removedPixels = cleanupCloudsBasedOnNearby(tensorflowMaskImage, width, height, 2);
        tfCloudPixels -= removedPixels;
        
        removedPixels = cleanupCloudsBasedOnTiles(tensorflowMaskImage, width, height, 100, 3);
        tfCloudPixels -= removedPixels;
        
        return new ImageDetectionResultDTO(null, null, (int) tfCheckedPixels, (int) tfCloudPixels, tfCloudPixels / tfCheckedPixels);
    }
    
    public void saveTensorflowMaskImage(final File file) throws IOException {
        ImageIO.write(tensorflowMaskImage, "png", file);
    }
    
    private int checkTileData(final BufferedImage image, final int startWidth, final int startHeight, final int tileWidth, final int tileHeight, final int components) {
        int thisTileCloudPixels = 0;
        int[] dnValues = getImageTileDataFromCoordinates(image, tileWidth, tileHeight, components, startWidth, startHeight);
        if (isEmptyTile(componentMaxValue, dnValues)) {
            log.trace(String.format("[%20s] tile_l:[%04d,%04d] skipping...", name, startWidth, startHeight));
            for (int j = 0; j < TILE_WIDTH; j++) {
                for (int k = 0; k < TILE_HEIGHT; k++) {
                    tensorflowMaskImage.setRGB(startWidth + k, startHeight + j, BLACK_RGB);
                }
            }
        } else {
            log.trace(String.format("[%20s] tile_l:[%04d,%04d] detecting...", name, startWidth, startHeight));
            
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
        return detectorApiClient.checkImageFile(dataFile.getAbsolutePath(), new File(FileNameUtils.getImageCloudCoverMaskFilename(cloudMaskDir, parentDirName, name)).getAbsolutePath());
    }
    
    
    private void parseImagePixels( int width, final int height, final BufferedImage jImage, final boolean updateHistogram, final boolean updateContrast, final boolean detectClouds) {
        int heightStep = 100;
        for (int heightStart = 0; heightStart < height; heightStart += heightStep) {
            final int size = width * heightStep;
            int[] dnValues = new int[size];
            log.debug("{} {}", jImage.getSampleModel(), jImage.getSampleModel().getClass());
            dnValues = jImage.getRGB(0, heightStart, width, heightStep, dnValues, 0, width);
            for (int i = 0; i < size; i++) {
                final Color color = new Color(dnValues[i], false);
                final int x = (i) % width;
                final int y = (i) / width + heightStart;
                if (isValidPixel(256, color)) {
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
        }
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
    
    public void cleanup() {
        if (uncompressedImageFile != null) {
            if (!uncompressedImageFile.delete()) {
                log.warn("[{}] failed to delete uncompressed image file!", name);
            }
        }
    }
}
