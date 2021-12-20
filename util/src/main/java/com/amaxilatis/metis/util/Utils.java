package com.amaxilatis.metis.util;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.model.WorldFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.image.TiffParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.amaxilatis.metis.config.Conditions.N1_PIXEL_SIZE;
import static com.amaxilatis.metis.config.Conditions.N2_BIT_SIZE;
import static com.amaxilatis.metis.config.Conditions.N3_SAMPLES_PER_PIXEL;

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
            long start = System.currentTimeMillis();
            final BodyContentHandler handler = new BodyContentHandler();
            final FileInputStream inputStream = new FileInputStream(file);
            final ParseContext context = new ParseContext();
            final TiffParser JpegParser = new TiffParser();
            JpegParser.parse(inputStream, handler, metadata, context);
            log.info("[{}] jpegParser took {}ms", file.getName(), (System.currentTimeMillis() - start));
            
            if (tasks.contains(0)) {
                try {
                    Utils.parseFile(file, handler, metadata, context);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(1)) {
                try {
                    results.add(Utils.testN1(file, handler, metadata, context));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(2)) {
                try {
                    results.add(Utils.testN2(file, handler, metadata, context));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(3)) {
                try {
                    results.add(Utils.testN3(file, handler, metadata, context));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return results;
    }
    
    public static void parseFile(final File file, final BodyContentHandler handler, final Metadata metadata, final ParseContext context) {
        final Map<String, Object> map = Arrays.stream(metadata.names()).collect(Collectors.toMap(name -> name, metadata::get, (a, b) -> b, HashMap::new));
        log.debug("[{}] Metadata: {}", file.getName(), map);
    }
    
    /**
     * 1. Έλεγχος της χωρικής ανάλυσης όπου θα διαπιστωθεί ότι ο λόγος της τελικής ανάλυσης της ορθοαναγωγής
     * προς την απόσταση δειγματοληψίας εδάφους (απόσταση μεταξύ δύο διαδοχικών κέντρων εικονοστοιχείων
     * που μετριούνται στο έδαφος) είναι σύμφωνα με τις προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN1(final File file, final BodyContentHandler handler, final Metadata metadata, final ParseContext context) {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(1);
        
        final File worldFileFile = getWorldFile(file);
        final WorldFile worldFile = parseWorldFile(worldFileFile);
        
        boolean worldConditionRes = evaluateWorldFile(worldFile);
        boolean metadataRes = true;
        
        log.info("[N1] file:{}, n1:{} world", file.getName(), worldConditionRes);
        
        for (final String metadataName : metadata.names()) {
            log.debug("metadataName: " + metadataName);
            if (metadataName.contains("0x830e")) {
                final String metadataValue = metadata.get(metadataName);
                log.debug("[N1] file:{}, {}:{} ", file.getName(), metadataName, metadataValue);
                final String[] pixelSizes = metadataValue.replaceAll(",", "\\.").split(" ");
                
                for (final String pixelSize : pixelSizes) {
                    if (Double.parseDouble(pixelSizes[0]) > N1_PIXEL_SIZE || Double.parseDouble(pixelSizes[2]) > N1_PIXEL_SIZE) {
                        metadataRes = false;
                    }
                }
                log.info("[N1] file:{}, n1:{} exif", file.getName(), metadataRes);
                resultBuilder.note(metadataName + ": " + metadataValue);
            }
        }
        return resultBuilder.result(worldConditionRes && metadataRes).build();
    }
    
    private static boolean evaluateWorldFile(final WorldFile worldFile) {
        return worldFile.getXPixelSize() == 0.5 && worldFile.getXRotation() == 0 && worldFile.getYRotation() == 0 && worldFile.getYPixelSize() == -0.5 && ((int) (worldFile.getXCenter() * 100) % 100) == 25 && ((int) (worldFile.getYCenter() * 100) % 100) == 75;
    }
    
    public static File getWorldFile(final File file) {
        final File parentFile = file.getParentFile();
        if (file.getName().endsWith(".tif")) {
            return new File(parentFile, file.getName().replaceAll("tif$", "tfw"));
        } else if (file.getName().endsWith(".jpg")) {
            return new File(parentFile, file.getName().replaceAll("jpg$", "jgw"));
        } else if (file.getName().endsWith(".ecw")) {
            return new File(parentFile, file.getName().replaceAll("ecw$", "eww"));
        } else if (file.getName().endsWith(".jp2")) {
            return new File(parentFile, file.getName().replaceAll("jp2$", "j2w"));
        } else {
            return null;
        }
    }
    
    public static WorldFile parseWorldFile(final File wFile) {
        final List<Object> lines = new ArrayList<>();
        try (final BufferedReader br = new BufferedReader(new FileReader(wFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return WorldFile.builder().xPixelSize(Double.parseDouble((String) lines.get(0))).yRotation(Double.parseDouble((String) lines.get(1))).xRotation(Double.parseDouble((String) lines.get(2))).yPixelSize(Double.parseDouble((String) lines.get(3))).xCenter(Double.parseDouble((String) lines.get(4))).yCenter(Double.parseDouble((String) lines.get(5))).build();
    }
    
    /**
     * 2. Έλεγχος της ραδιομετρικής ανάλυσης όπου θα επαληθευτεί ότι είναι 11-12 bits ανά κανάλι σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN2(final File file, final BodyContentHandler handler, final Metadata metadata, final ParseContext context) {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(2);
        for (final String metadataName : metadata.names()) {
            if (metadataName.contains("Exif IFD0:Bits Per Sample")) {
                final String metadataValue = metadata.get(metadataName).replaceAll("[^0-9 ]", "");
                log.debug("[N2] file:{}, {}:{} ", file.getName(), metadataName, metadataValue);
                final String[] bitsCounts = metadataValue.split(" ");
                boolean metadataTest = true;
                for (final String bitsCount : bitsCounts) {
                    if (Integer.parseInt(bitsCount) < N2_BIT_SIZE) {
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
     */
    public static FileJobResult testN3(final File file, final BodyContentHandler handler, final Metadata metadata, final ParseContext context) {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(3);
        for (final String metadataName : metadata.names()) {
            if (metadataName.contains("tiff:SamplesPerPixel")) {
                //if (name.contains("Exif IFD0:Samples Per Pixel")) {
                //if (name.contains("Exif IFD0:Photometric Interpretation")) {
                final String metadataValue = metadata.get(metadataName);
                log.debug("[N3] file:{}, {}:{} ", file.getName(), metadataName, metadataValue);
                final int samplesPerPixel = Integer.parseInt(metadataValue);
                final boolean metadataTest = samplesPerPixel == N3_SAMPLES_PER_PIXEL;
                log.info("[N3] file:{}, n3:{}", file.getName(), metadataTest);
                return resultBuilder.result(metadataTest).note(metadataName + ": " + metadataValue).build();
            }
        }
        return resultBuilder.result(false).build();
    }
}
