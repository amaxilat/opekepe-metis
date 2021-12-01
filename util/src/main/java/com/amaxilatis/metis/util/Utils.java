package com.amaxilatis.metis.util;

import com.amaxilatis.metis.util.model.FileJobResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.image.TiffParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class Utils {
    public static final String NAME = "METIS";
    
    public static List<FileJobResult> parseDir(final File directory, final List<Integer> tasks) throws IOException, TikaException, SAXException {
        final List<FileJobResult> results = new ArrayList<>();
        for (final File file : Objects.requireNonNull(directory.listFiles())) {
            results.addAll(parseFile(file, tasks));
        }
        return results;
    }
    
    public static List<FileJobResult> parseFile(final File file, final List<Integer> tasks) throws IOException, TikaException, SAXException {
        final Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "application/octet-stream");
        final List<FileJobResult> results = new ArrayList<>();
        
        if (file.getName().endsWith(".tif")) {
            log.info("[{}] parsing...", file.getName());
            final BodyContentHandler handler = new BodyContentHandler();
            final FileInputStream inputstream = new FileInputStream(file);
            final ParseContext pcontext = new ParseContext();
            final TiffParser JpegParser = new TiffParser();
            JpegParser.parse(inputstream, handler, metadata, pcontext);
            
            
            if (tasks.contains(0)) {
                try {
                    Utils.parseFile(file, handler, metadata, pcontext);
                } catch (IOException | TikaException | SAXException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(1)) {
                try {
                    results.add(Utils.testN1(file, handler, metadata, pcontext));
                } catch (IOException | TikaException | SAXException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(2)) {
                try {
                    results.add(Utils.testN2(file, handler, metadata, pcontext));
                } catch (IOException | TikaException | SAXException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(3)) {
                try {
                    results.add(Utils.testN3(file, handler, metadata, pcontext));
                } catch (IOException | TikaException | SAXException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return results;
    }
    
    public static List<FileJobResult> parseFile(final File file, final BodyContentHandler handler, final Metadata metadata, final ParseContext pcontext) throws IOException, TikaException, SAXException {
        //final String[] metadataNames = metadata.names();
        //Arrays.stream(metadataNames).forEach(name -> log.info("{}: {}", name, metadata.get(name)));
        final Map<String, Object> map = Arrays.stream(metadata.names()).collect(Collectors.toMap(name -> name, metadata::get, (a, b) -> b, HashMap::new));
        log.info("[{}] Metadata: {}", file.getName(), map);
        return new ArrayList<>();
    }
    
    /**
     * 1. Έλεγχος της χωρικής ανάλυσης όπου θα διαπιστωθεί ότι ο λόγος της τελικής ανάλυσης της ορθοαναγωγής
     * προς την απόσταση δειγματοληψίας εδάφους (απόσταση μεταξύ δύο διαδοχικών κέντρων εικονοστοιχείων
     * που μετριούνται στο έδαφος) είναι σύμφωνα με τις προδιαγραφές(*),
     *
     * @param file
     * @return
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public static FileJobResult testN1(final File file, final BodyContentHandler handler, final Metadata metadata, final ParseContext pcontext) throws IOException, TikaException, SAXException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(1);
        for (final String metadataName : metadata.names()) {
            if (metadataName.contains("0x830e")) {
                final String metadataValue = metadata.get(metadataName);
                log.debug("[N1] file:{}, {}:{} ", file.getName(), metadataName, metadataValue);
                final String[] pixelSizes = metadataValue.replaceAll(",", "\\.").split(" ");
                boolean metadataTest = true;
                for (final String pixelSize : pixelSizes) {
                    if (Double.parseDouble(pixelSizes[0]) > 0.5 || Double.parseDouble(pixelSizes[2]) > 0.5) {
                        metadataTest = false;
                    }
                }
                log.info("[N1] file:{}, n1:{}", file.getName(), metadataTest);
                return resultBuilder.result(metadataTest).note(metadataName + ": " + metadataValue).build();
            }
        }
        return resultBuilder.result(false).build();
    }
    
    /**
     * 2. Έλεγχος της ραδιομετρικής ανάλυσης όπου θα επαληθευτεί ότι είναι 11-12 bits ανά κανάλι σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file
     * @return
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public static FileJobResult testN2(final File file, final BodyContentHandler handler, final Metadata metadata, final ParseContext pcontext) throws IOException, TikaException, SAXException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(2);
        for (final String metadataName : metadata.names()) {
            if (metadataName.contains("Exif IFD0:Bits Per Sample")) {
                final String metadataValue = metadata.get(metadataName).replaceAll("[^0-9 ]", "");
                log.debug("[N2] file:{}, {}:{} ", file.getName(), metadataName, metadataValue);
                String[] bitsCounts = metadataValue.split(" ");
                boolean metadataTest = true;
                for (String bitsCount : bitsCounts) {
                    Integer bitCountInt = Integer.valueOf(bitsCount);
                    if (bitCountInt != 11 && bitCountInt != 12) {
                        metadataTest = false;
                    }
                }
                log.info("[N2] file:{}, n2:{}", file.getName(), metadataTest);
                return resultBuilder.result(metadataTest).note(metadataName + ": " + metadataValue).build();
            }
        }
        return resultBuilder.result(false).build();
    }
    
    /**
     * * 3. Έλεγχος της φασματικής ανάλυσης όπου θα διαπιστωθεί ότι το πλήθος των καναλιών είναι σύμφωνο με τα
     * στοιχεία παράδοσης και της προδιαγραφές(*),
     *
     * @param file
     * @return
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public static FileJobResult testN3(final File file, final BodyContentHandler handler, final Metadata metadata, final ParseContext pcontext) throws IOException, TikaException, SAXException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(3);
        for (final String metadataName : metadata.names()) {
            if (metadataName.contains("tiff:SamplesPerPixel")) {
                //if (name.contains("Exif IFD0:Samples Per Pixel")) {
                //if (name.contains("Exif IFD0:Photometric Interpretation")) {
                final String metadataValue = metadata.get(metadataName);
                log.debug("[N3] file:{}, {}:{} ", file.getName(), metadataName, metadataValue);
                final boolean metadataTest = metadataValue.equals("4");
                log.info("[N3] file:{}, n3:{}", file.getName(), metadataTest);
                return resultBuilder.result(metadataTest).note(metadataName + ": " + metadataValue).build();
            }
        }
        return resultBuilder.result(false).build();
    }
}
