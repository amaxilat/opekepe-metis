package com.amaxilatis.metis.model;


import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.image.TiffParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
    
    public ImagePack(final File file) throws IOException {
        this.ioMetadata = null;
        this.image = null;
        this.metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, OCTET_STREAM);
        this.handler = new BodyContentHandler();
        this.inputStream = new FileInputStream(file);
        this.context = new ParseContext();
        this.file = file;
        
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
            final int components = jImage.getColorModel().getNumComponents();
            final int width = jImage.getWidth();
            final int height = jImage.getHeight();
            final int heightStep = 810;
            int heightStart = 0;
            int currentStep = heightStep;
            do {
                if (heightStart + currentStep > height) {
                    currentStep = height - heightStart;
                }
                final int size = width * heightStep * components;
                int dnValues[] = new int[size];
                dnValues = jImage.getData().getPixels(0, heightStart, width, currentStep, dnValues);
                for (int i = 0; i < size; i += components) {
                    if ((255 != dnValues[i] || 255 != dnValues[i + 1] || 255 != dnValues[i + 2] || 255 != dnValues[i + 3]) && (0 != dnValues[i] || 0 != dnValues[i + 1] || 0 != dnValues[i + 2] || 0 != dnValues[i + 3])) {
                        histogram.addValues(dnValues[i], dnValues[i + 1], dnValues[i + 2], dnValues[i + 3]);
                    }
                }
                heightStart += heightStep;
            } while (heightStart <= height);
            
            this.histogramLoaded = true;
        }
    }
}
