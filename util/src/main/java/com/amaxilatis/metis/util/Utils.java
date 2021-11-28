package com.amaxilatis.metis.util;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class Utils {
    public static final String NAME = "METIS";
    
    public static void parse(final File directory) throws IOException, TikaException, SAXException {
        
        for (final File file : Objects.requireNonNull(directory.listFiles())) {
            parseFile(file);
        }
    }
    
    public static void parseFile(final File file) throws IOException, TikaException, SAXException {
        final Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "application/octet-stream");
        
        if (file.getName().endsWith(".tif")) {
            log.info("[{}] parsing...", file.getName());
            final BodyContentHandler handler = new BodyContentHandler();
            final FileInputStream inputstream = new FileInputStream(file);
            final ParseContext pcontext = new ParseContext();
            final TiffParser JpegParser = new TiffParser();
            JpegParser.parse(inputstream, handler, metadata, pcontext);
            //final String[] metadataNames = metadata.names();
            //Arrays.stream(metadataNames).forEach(name -> log.info("{}: {}", name, metadata.get(name)));
            final Map<String, Object> map = Arrays.stream(metadata.names()).collect(Collectors.toMap(name -> name, metadata::get, (a, b) -> b, HashMap::new));
            log.info("[{}] Metadata: {}", file.getName(), map);
        }
    }
    
    /**
     * 1. Έλεγχος της χωρικής ανάλυσης όπου θα διαπιστωθεί ότι ο λόγος της τελικής ανάλυσης της ορθοαναγωγής
     * προς την απόσταση δειγματοληψίας εδάφους (απόσταση μεταξύ δύο διαδοχικών κέντρων εικονοστοιχείων
     * που μετριούνται στο έδαφος) είναι σύμφωνα με τις προδιαγραφές(*),
     *
     * @param file
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public static void testN1(final File file) throws IOException, TikaException, SAXException {
        log.info("[N1] {}", file.getName());
    }
    
    /**
     * 2. Έλεγχος της ραδιομετρικής ανάλυσης όπου θα επαληθευτεί ότι είναι 11-12 bits ανά κανάλι σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public static void testN2(final File file) throws IOException, TikaException, SAXException {
        log.info("[N2] {}", file.getName());
    }
    
    /**
     * * 3. Έλεγχος της φασματικής ανάλυσης όπου θα διαπιστωθεί ότι το πλήθος των καναλιών είναι σύμφωνο με τα
     * στοιχεία παράδοσης και της προδιαγραφές(*),
     *
     * @param file
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public static void testN3(final File file) throws IOException, TikaException, SAXException {
        log.info("[N3] {}", file.getName());
    }
}
