package com.amaxilatis.metis.model;


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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.amaxilatis.metis.util.CloudUtils.BLACK_RGB;
import static com.amaxilatis.metis.util.CloudUtils.GRAY_RGB;
import static com.amaxilatis.metis.util.CloudUtils.WHITE_RGB;
import static com.amaxilatis.metis.util.CloudUtils.cleanupNDWI;
import static com.amaxilatis.metis.util.ColorUtils.getBSI;
import static com.amaxilatis.metis.util.ColorUtils.getNDWI;
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
    private SummaryStatistics dnStats;
    @Getter
    private double validPixels = 0;
    @Getter
    private double cloudPixels = 0;
    private BufferedImage colorBalanceMask;
    @Getter
    private SummaryStatistics colorBalanceStatistics;
    @Getter
    private final int compressionExifValue;
    private BufferedImage cloudDetectionMask;
    private BufferedImage nirMask;
    private BufferedImage bsiMask;
    private BufferedImage ndwiMask;
    private final String cloudMaskDir;
    private final int workers;
    @Getter
    private int componentMaxValue;
    @Getter
    private double redSnr;
    @Getter
    private double greenSrn;
    @Getter
    private double blueSnr;
    
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
            final IIOImage iIOImage = new IIOImage(bufferedImage, null, null);
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
            log.debug("[{}][loadImage] took: {}", name, (System.currentTimeMillis() - start));
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
            
            final int componentSize = image.getColorModel().getPixelSize() / image.getColorModel().getNumComponents();
            componentMaxValue = (int) Math.pow(2, componentSize);
            
            //histogram
            //values added to histogram are always scaled to 0-256
            this.histogram = new HistogramsHelper(256);
            //image information
            final int width = jImage.getWidth();
            final int height = jImage.getHeight();
            // pixel value statistics
            this.dnStats = new SummaryStatistics();
            this.colorBalanceStatistics = new SummaryStatistics();
            
            //mask images for color balance calculation
            this.colorBalanceMask = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            //do a pass for histogram and contrast data
            parseImagePixels(jImage);
            
            this.histogramLoaded = true;
        }
    }
    
    /**
     * Parse the image and generate its color balance
     *
     * @throws IOException
     */
    public void loadColorBalance() throws IOException, ImageProcessingException, TikaException, SAXException {
        loadHistogram();
    }
    
    /**
     * Parse the image and generate its histogram
     *
     * @throws IOException
     */
    public void detectClouds() throws IOException {
        final BufferedImage jImage = ImageIO.read(dataFile);
        //image information
        final int width = jImage.getWidth();
        final int height = jImage.getHeight();
        
        //mask images for cloud detection
        this.cloudDetectionMask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        this.nirMask = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.bsiMask = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.ndwiMask = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        final long start = System.currentTimeMillis();
        log.info("[{}] cloud detection...", name);
        
        final CloudDetectionResult detectionResult = detectCloudsInTilesOfImage(jImage, width, height);
        
        //get results form the call
        validPixels = detectionResult.getPixels();
        cloudPixels = detectionResult.getCloudy();
        
        double percentage = cloudPixels / validPixels;
        log.info(String.format("[%s][N4] cloud-detection result: %1.3f took: %d sec", name, percentage, (System.currentTimeMillis() - start) / 1000));
    }
    
    private CloudDetectionResult detectCloudsInTilesOfImage(final BufferedImage image, final int width, final int height) throws IOException {
        double tfCheckedPixels = 0;
        double tfCloudPixels = 0;
        
        int components = 4;
        //paint the whole image gray - not yet processed
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                cloudDetectionMask.setRGB(w, h, GRAY_RGB);
                nirMask.setRGB(w, h, GRAY_RGB);
                bsiMask.setRGB(w, h, BLACK_RGB);
                ndwiMask.setRGB(w, h, GRAY_RGB);
            }
        }
        
        final ExecutorService executorService = Executors.newFixedThreadPool(workers);
        final List<Future<Integer>> futures = new ArrayList<>();
        
        final int widthTiles = width / TILE_WIDTH;
        final int heightTiles = height / TILE_HEIGHT;
        
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
            executorService.awaitTermination(24, TimeUnit.HOURS);
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
    
        tfCloudPixels -= cleanupNDWI(cloudDetectionMask, 0, 0, image.getWidth(), image.getHeight());
        
        return new CloudDetectionResult(null, null, (int) tfCheckedPixels, (int) tfCloudPixels, tfCloudPixels / tfCheckedPixels);
    }
    
    public void saveTensorflowMaskImage(final File file) throws IOException {
        ImageIO.write(cloudDetectionMask, "png", file);
        ImageIO.write(nirMask, "png", new File(file.getParentFile(), file.getName().replace("mask", "nir")));
        ImageIO.write(bsiMask, "png", new File(file.getParentFile(), file.getName().replace("mask", "bsi")));
        ImageIO.write(ndwiMask, "png", new File(file.getParentFile(), file.getName().replace("mask", "ndwi")));
    }
    
    /**
     * Save the color balance mask image.
     *
     * @param file the file for storing the color balance mask image.
     * @throws IOException
     */
    public void saveColorBalanceMaskImage(final File file) throws IOException {
        ImageIO.write(colorBalanceMask, "png", file);
    }
    
    /**
     * Check the provided image tile for clouds.
     *
     * @param image       the satellite image.
     * @param startWidth  the x location to start detection
     * @param startHeight the y location to start detection
     * @param tileWidth   the width of the tile to scan
     * @param tileHeight  the height of the tile to scan
     * @param components  the number of bands in the image
     * @return the number of cloud pixels found.
     */
    private int checkTileData(final BufferedImage image, final int startWidth, final int startHeight, final int tileWidth, final int tileHeight, final int components) {
        int thisTileCloudPixels = 0;
        int[] dnValues = getImageTileDataFromCoordinates(image, tileWidth, tileHeight, components, startWidth, startHeight);
        if (isEmptyTile(componentMaxValue, dnValues)) {
            log.trace("[{}][N4] tile:[{},{}] skipping empty tile...", name, startWidth, startHeight);
        } else {
            log.trace("[{}][N4] tile:[{},{}] detecting...", name, startWidth, startHeight);
            for (int i = 0; i < dnValues.length; i += components) {
                final int x = startWidth + ((i / components) % (tileWidth));
                final int y = startHeight + ((i / components) / (tileWidth));
                final int r = dnValues[i], g = dnValues[i + 1], b = dnValues[i + 2], nir = dnValues[i + 3];
                
                nirMask.setRGB(x, y, new Color(nir, nir, nir).getRGB());
                
                final int bsi = (int) (getBSI(nir, dnValues[i], dnValues[i + 2]) * 255);
                bsiMask.setRGB(x, y, new Color(bsi, bsi, bsi).getRGB());
                
                int ndwi = -999;
                try {
                    ndwi = (int) (getNDWI(nir, g) * 255);
                    ndwiMask.setRGB(x, y, new Color(ndwi, ndwi, ndwi).getRGB());
                    final boolean isCloudPixel = (124 < ndwi && ndwi < 140) && (84 < bsi && bsi < 96);
                    thisTileCloudPixels += isCloudPixel ? 1 : 0;
                    cloudDetectionMask.setRGB(x, y, isCloudPixel ? WHITE_RGB : BLACK_RGB);
                } catch (IllegalArgumentException e) {
                    log.error("IllegalArgumentException: {}", ndwi);
                } catch (ArithmeticException e) {
                    log.error("ArithmeticException: {} {}", g, nir);
                }
            }
        }
        if (thisTileCloudPixels > 0) {
            log.debug("[{}][N4] tile:[{},{}] has clouds in {} pixels", name, startWidth, startHeight, thisTileCloudPixels);
        }
        return thisTileCloudPixels;
    }
    
    private void parseImagePixels(final BufferedImage jImage) {
        final int width = jImage.getWidth();
        final int height = jImage.getHeight();
        long validPixelCount = 0;
        int heightStep = 100;
        for (int heightStart = 0; heightStart < height; heightStart += heightStep) {
            final int size = width * heightStep;
            int[] dnValues = new int[size];
            dnValues = jImage.getRGB(0, heightStart, width, heightStep, dnValues, 0, width);
            for (int i = 0; i < size; i++) {
                final Color color = new Color(dnValues[i], false);
                if (isValidPixel(256, color)) {
                    //update histogram for check 5,6
                    updateHistogram(color);
                    //update the cloud data for check 7
                    updateContrastData(color);
                    
                    final int x = (i) % width;
                    final int y = (i) / width + heightStart;
                    updateColorBalance(x, y, color);
                    
                    validPixelCount++;
                }
            }
        }
        final int cutoff1 = (int) (validPixelCount * 0.25);
        //red
        redSnr = calcSnr(ColorUtils.LAYERS.RED, cutoff1);
        //green
        greenSrn = calcSnr(ColorUtils.LAYERS.GREEN, cutoff1);
        //blue
        blueSnr = calcSnr(ColorUtils.LAYERS.BLUE, cutoff1);
    }
    
    private double calcSnr(final ColorUtils.LAYERS color, final int samplesOfCutoff) {
        int bandCutoff = 0;
        int i = 0;
        final SummaryStatistics bandStats = new SummaryStatistics();
        try {
            while (bandCutoff < samplesOfCutoff && i < 255) {
                i++;
                bandCutoff += getHistogram().getBins().get(color).getData()[i];
            }
            if (bandCutoff > samplesOfCutoff) {
                //add the elements in the band that were cut off incorrectly (this happens when the last bin has more elements than the cutoff1 threshold.
                for (int excessCutoff = samplesOfCutoff; excessCutoff < bandCutoff; excessCutoff++) {
                    bandStats.addValue(i);
                }
            }
            for (i++; i < 256; i++) {
                //add all the elements of the bucket
                try {
                    for (int k = 0; k < getHistogram().getBins().get(color).getData()[i]; k++) {
                        bandStats.addValue(i);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    log.error("[{}][calcSnr] color={}, i={}, len(data)={}", name, color, i, getHistogram().getBins().get(color).getData().length, e);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("[{}][calcSnr] color={}, i={}, len(data)={}", name, color, i, getHistogram().getBins().get(color).getData().length, e);
        }
        log.info("[{}][calcSnr] band: {} | {} | {} {} {} | {}", name, color, bandStats.getN(), bandStats.getMax(), bandStats.getMin(), bandStats.getMean(), bandStats.getStandardDeviation());
        return (bandStats.getMean() / bandStats.getStandardDeviation()) * 100;
    }
    
    /**
     * Update the calculated color balance data.
     *
     * @param x     the x coordinate of the pixel
     * @param y     the y coordinate of the pixel
     * @param color the color of the pixel
     */
    private void updateColorBalance(final int x, final int y, final Color color) {
        final int alpha = Math.max(Math.max(color.getRed(), color.getGreen()), color.getBlue());
        final int beta = Math.min(Math.min(color.getRed(), color.getGreen()), color.getBlue());
        double gama = 0;
        if (alpha > 0) {
            gama = ((double) (alpha - beta)) / alpha;
        }
        final int maskValue = (int) (gama * 256);
        colorBalanceMask.setRGB(x, y, maskValue);
        colorBalanceStatistics.addValue(gama);
    }
    
    
    private void updateHistogram(final Color color) {
        histogram.addValues(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }
    
    private void updateContrastData(final Color color) {
        final double brightness = ColorUtils.getBrightness(color);
        dnStats.addValue(brightness);
    }
    
    public void cleanup() {
        if (uncompressedImageFile != null) {
            if (!uncompressedImageFile.delete()) {
                log.warn("[{}] failed to delete uncompressed image file!", name);
            }
        }
    }
}
