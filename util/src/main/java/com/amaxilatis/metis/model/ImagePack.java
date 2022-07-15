package com.amaxilatis.metis.model;


import com.amaxilatis.metis.util.ColorUtils;
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
    private String histogramDir;
    
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
            this.histogram = new HistogramsHelper();
            this.dnValuesStatistics = new SummaryStatistics();
            final int components = jImage.getColorModel().getNumComponents();
            final int width = jImage.getWidth();
            final int height = jImage.getHeight();
            
            this.maskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            
            final int heightStep = height / 10;
            int heightStart = 0;
            int currentStep = heightStep;
            do {
                if (heightStart + currentStep > height) {
                    currentStep = height - heightStart;
                }
                if (currentStep == 0) {
                    break;
                }
                final int size = width * heightStep * components;
                int dnValues[] = new int[size];
                dnValues = jImage.getData().getPixels(0, heightStart, width, currentStep, dnValues);
                int x = 0;
                int y = 0;
                for (int i = 0; i < size; i += components) {
                    final int r = dnValues[i];
                    final int g = dnValues[i + 1];
                    final int b = dnValues[i + 2];
                    final int nir = dnValues[i + 3];
                    x = (i / 4) % width;
                    y = (i / 4) / width + heightStart;
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
            
            //write mask file to storage
            ImageIO.write(maskImage, "png", new File(histogramDir, this.file.getName() + ".mask.png"));
            
            this.histogramLoaded = true;
        }
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
