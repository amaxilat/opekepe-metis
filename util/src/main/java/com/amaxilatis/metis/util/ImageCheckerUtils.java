package com.amaxilatis.metis.util;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.model.HistogramBin;
import com.amaxilatis.metis.model.ImagePack;
import com.amaxilatis.metis.model.WorldFile;
import com.amaxilatis.metis.model.WorldFileResult;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.amaxilatis.metis.config.Conditions.N1_PIXEL_SIZE;
import static com.amaxilatis.metis.config.Conditions.N2_BIT_SIZE;
import static com.amaxilatis.metis.config.Conditions.N3_SAMPLES_PER_PIXEL;
import static com.amaxilatis.metis.util.FileUtils.getResultFile;
import static com.amaxilatis.metis.util.WorldFileUtils.evaluateWorldFile;
import static com.amaxilatis.metis.util.WorldFileUtils.getWorldFile;
import static com.amaxilatis.metis.util.WorldFileUtils.parseWorldFile;
import static com.drew.metadata.exif.ExifDirectoryBase.TAG_BITS_PER_SAMPLE;
import static com.drew.metadata.exif.ExifDirectoryBase.TAG_COMPRESSION;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;

@Slf4j
public class ImageCheckerUtils {
    public static final String NAME = "METIS";
    public static final ObjectMapper mapper = new ObjectMapper();
    
    public static List<FileJobResult> parseDir(final File directory, final List<Integer> tasks) throws IOException, TikaException, SAXException, ImageProcessingException {
        final List<FileJobResult> results = new ArrayList<>();
        for (final File file : Objects.requireNonNull(directory.listFiles())) {
            results.addAll(parseFile(file, tasks, null));
        }
        return results;
    }
    
    public static List<FileJobResult> parseFile(final File file, final List<Integer> tasks, final String resultsDir) throws IOException, TikaException, SAXException, ImageProcessingException {
        final List<FileJobResult> results = new ArrayList<>();
        
        if (file.getName().endsWith(".tif") || file.getName().endsWith(".jpf")) {
            log.info("[{}] parsing...", file.getName());
            ImagePack image = new ImagePack(file);
            
            if (tasks.contains(1)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 1);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 1 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 1 for {}", file);
                        result = ImageCheckerUtils.testN1(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(2)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 2);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 2 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 2 for {}", file);
                        result = ImageCheckerUtils.testN2(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(3)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 3);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 3 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 3 for {}", file);
                        result = ImageCheckerUtils.testN3(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(4)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 4);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 4 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 4 for {}", file);
                        result = ImageCheckerUtils.testN4(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(5)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 5);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 5 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 5 for {}", file);
                        result = ImageCheckerUtils.testN5(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(6)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 6);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 6 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 6 for {}", file);
                        result = ImageCheckerUtils.testN6(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(7)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 7);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 7 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 7 for {}", file);
                        result = ImageCheckerUtils.testN7(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(8)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 8);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 8 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 8 for {}", file);
                        result = ImageCheckerUtils.testN8(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (tasks.contains(9)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 9);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 9 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 9 for {}", file);
                        result = ImageCheckerUtils.testN9(file, image);
                        if (resultsDir != null) {
                            mapper.writeValue(resultFile, result);
                        }
                    }
                    results.add(result);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return results;
    }
    
    /**
     * 1. Έλεγχος της χωρικής ανάλυσης όπου θα διαπιστωθεί ότι ο λόγος της τελικής ανάλυσης της ορθοαναγωγής
     * προς την απόσταση δειγματοληψίας εδάφους (απόσταση μεταξύ δύο διαδοχικών κέντρων εικονοστοιχείων
     * που μετριούνται στο έδαφος) είναι σύμφωνα με τις προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN1(final File file, final ImagePack image) throws TikaException, IOException, SAXException {
        image.loadImage();
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(1);
        
        final File worldFileFile = getWorldFile(file);
        final WorldFile worldFile = parseWorldFile(worldFileFile);
        
        final WorldFileResult worldConditionRes = evaluateWorldFile(worldFile);
        boolean metadataRes = true;
        
        log.info("[N1] file:{}, n1:{} world", file.getName(), worldConditionRes.isOk());
        StringBuilder note = new StringBuilder();
        note.append("world: ");
        note.append(worldConditionRes.getNote());
        note.append(" exif: ");
        for (final String metadataName : image.getMetadata().names()) {
            log.debug("metadataName: " + metadataName);
            if (metadataName.contains("0x830e")) {
                final String metadataValue = image.getMetadata().get(metadataName);
                log.debug("[N1] file:{}, {}:{} ", file.getName(), metadataName, metadataValue);
                final String[] pixelSizes = metadataValue.replaceAll(",", "\\.").split(" ");
                
                double doublePixelSize0 = Double.parseDouble(pixelSizes[0]);
                double doublePixelSize1 = Double.parseDouble(pixelSizes[2]);
                if (doublePixelSize0 > N1_PIXEL_SIZE || doublePixelSize1 > N1_PIXEL_SIZE) {
                    metadataRes = false;
                }
                note.append(doublePixelSize0);
                note.append(",");
                note.append(doublePixelSize1);
                log.info("[N1] file:{}, n1:{} exif", file.getName(), metadataRes);
                resultBuilder.note(note.toString());
            }
        }
        return resultBuilder.result(worldConditionRes.isOk() && metadataRes).build();
    }
    
    /**
     * 2. Έλεγχος της ραδιομετρικής ανάλυσης όπου θα επαληθευτεί ότι είναι 11-12 bits ανά κανάλι σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN2(final File file, final ImagePack image) throws IOException, ImageProcessingException {
        final BufferedImage jImage = image.getImage();
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(2);
        
        final StringBuilder note = new StringBuilder("");
        
        final ExifIFD0Directory directory = image.getIoMetadata().getFirstDirectoryOfType(ExifIFD0Directory.class);
        final String metadataValue = directory.getString(TAG_BITS_PER_SAMPLE).replaceAll("[^0-9 ]", "");
        log.info("[N2] bitPerSample {}", metadataValue);
        final String[] bitsCounts = metadataValue.split(" ");
        note.append("Exif bitsPerSample: ").append(metadataValue);
        boolean metadataTest = true;
        for (final String bitsCount : bitsCounts) {
            int bitsCountInt = Integer.parseInt(bitsCount);
            if (bitsCountInt < N2_BIT_SIZE) {
                metadataTest = false;
            }
        }
        
        log.debug("[N2] colorModelComponents: {}", jImage.getColorModel().getNumComponents());
        log.debug("[N2] colorModelPixelSize: {}", jImage.getColorModel().getPixelSize());
        final int pixelSize = jImage.getColorModel().getPixelSize() / jImage.getColorModel().getNumComponents();
        log.debug("[N2] bitPerPixel: {}", pixelSize);
        
        note.append(", Κανάλια: ").append(jImage.getColorModel().getNumComponents());
        note.append(", PixelSize: ").append(jImage.getColorModel().getPixelSize());
        note.append(", Size/Κανάλι: ").append(pixelSize);
        
        resultBuilder.note(note.toString()).result(metadataTest && (pixelSize >= N2_BIT_SIZE));
        
        return resultBuilder.build();
    }
    
    /**
     * * 3. Έλεγχος της φασματικής ανάλυσης όπου θα διαπιστωθεί ότι το πλήθος των καναλιών είναι σύμφωνο με τα
     * στοιχεία παράδοσης και της προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN3(final File file, final ImagePack image) throws IOException {
        final BufferedImage jImage = image.getImage();
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(3);
        
        StringBuilder note = new StringBuilder("");
        log.debug("[N3] colorModelComponents: {}", jImage.getColorModel().getNumComponents());
        log.debug("[N3] colorModelColorComponents: {}", jImage.getColorModel().getNumColorComponents());
        log.debug("[N3] colorModelHasAlpha: {}", jImage.getColorModel().hasAlpha());
        note.append("Κανάλια: ").append(jImage.getColorModel().getNumComponents()).append(", ");
        note.append("Χρώματα: ").append(jImage.getColorModel().getNumColorComponents()).append(", ");
        note.append("Alpha: ").append(jImage.getColorModel().hasAlpha()).append(", ");
        boolean result = jImage.getColorModel().getNumComponents() == N3_SAMPLES_PER_PIXEL && jImage.getColorModel().getNumColorComponents() == N3_SAMPLES_PER_PIXEL - 1 && jImage.getColorModel().hasAlpha();
        resultBuilder.result(result).note(note.toString()).build();
        
        return resultBuilder.build();
    }
    
    /**
     * * 4. Έλεγχος νεφοκάλυψης ανά εικόνα και συνολικά σε συμφωνία με τις προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN4(final File file, final ImagePack image) throws TikaException, IOException, SAXException {
        image.loadImage();
        image.loadHistogram();
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(4);
        return resultBuilder.result(false).build();
    }
    
    /**
     * * 5. Έλεγχος ολικού clipping το οποίο υπολογίζεται στο ιστόγραμμα φωτεινότητας σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN5(final File file, final ImagePack image) throws TikaException, IOException, SAXException {
        image.loadImage();
        image.loadHistogram();
        
        final double pixelsCount = image.getHistogram().getTotalPixels(ColorUtils.LAYERS.LUM);
        final Set<HistogramBin> top = image.getHistogram().getTop5Bins(ColorUtils.LAYERS.LUM);
        final Set<HistogramBin> bottom = image.getHistogram().getBottom5Bins(ColorUtils.LAYERS.LUM);
        long totalItemsInTop = top.stream().mapToLong(histogramBin -> histogramBin.getValuesCount()).sum();
        long totalItemsInBottom = bottom.stream().mapToLong(histogramBin -> histogramBin.getValuesCount()).sum();
        double topClipping = (totalItemsInTop / pixelsCount) * 100;
        double bottomClipping = (totalItemsInBottom / pixelsCount) * 100;
        log.info("top[{} - {}]: {}", totalItemsInTop, (totalItemsInTop / pixelsCount) * 100, top);
        log.info("bottom[{} - {}]: {}", totalItemsInBottom, (totalItemsInBottom / pixelsCount) * 100, bottom);
        
        boolean result = topClipping < 0.5 && bottomClipping < 0.5;
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(5);
        resultBuilder.note(String.format("Πρώτα: %.3f%% , Τελευταία: %.3f%%", topClipping, bottomClipping));
        return resultBuilder.result(result).build();
    }
    
    /**
     * * 6. Έλεγχος κορυφής ιστογράμματος από την τυπική μέση τιμή (πχ 8bit 128) και σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN6(final File file, final ImagePack image) throws IOException {
        image.loadHistogram();
        
        int histMinLimit = (int) (128 * 0.85);
        int histMaxLimit = (int) (128 * 1.15);
        log.info("brightness: {}< mean:{} <{} std: {}", histMinLimit, image.getHistogram().getMean(ColorUtils.LAYERS.LUM), histMaxLimit, image.getHistogram().getStandardDeviation(ColorUtils.LAYERS.LUM));
        final int majorBinCenterBr = image.getHistogram().majorBin(ColorUtils.LAYERS.LUM);
        log.info("histogramBr center: {}", majorBinCenterBr);
        boolean result = histMinLimit < majorBinCenterBr && majorBinCenterBr < histMaxLimit;
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(6);
        resultBuilder.note(String.format("Κέντρο: %d, όρια: [%d,%d]", majorBinCenterBr, histMinLimit, histMaxLimit));
        return resultBuilder.result(result).build();
    }
    
    /**
     * * 7. Έλεγχος αντίθεσης ανά κανάλι ως έλεγχος της μεταβλητότητας των ψηφιακών τιμών (DN) σαν ποσοστό των
     * διαθεσίμων επιπέδων του γκρι και σύμφωνα με τις προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN7(final File file, final ImagePack image) throws TikaException, IOException, SAXException {
        if (!image.isLoaded()) {
            image.loadImage();
        }
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(7);
        return resultBuilder.result(false).build();
    }
    
    /**
     * * 8. Έλεγχος συμπίεσης στον μορφότυπο των αρχείων (GeoTiff ή/και JPEG2000) και σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file
     * @return
     */
    public static FileJobResult testN8(final File file, final ImagePack image) throws IOException, ImageProcessingException {
        final ExifIFD0Directory directory = image.getIoMetadata().getFirstDirectoryOfType(ExifIFD0Directory.class);
        int compressionExifValue = Integer.parseInt(directory.getString(TAG_COMPRESSION));
        log.info("[N8] compressionExifValue: {}", compressionExifValue);
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(8);
        
        if (file.getName().endsWith(".tif")) {
            
            resultBuilder.note("Συμπίεση: " + CompressionUtils.toText(compressionExifValue));
            resultBuilder.result(CompressionUtils.isLossless(compressionExifValue));
            return resultBuilder.build();
        } else if (file.getName().endsWith(".jpf")) {
        }
        
        return resultBuilder.result(false).build();
    
//        1	= Uncompressed
//        2	= CCITT 1D
//        3	= T4/Group 3 Fax
//        4	= T6/Group 4 Fax
//        5	= LZW
//        6	= JPEG (old-style)
//        7	= JPEG
//        8	= Adobe Deflate
//        9	= JBIG B&W
//        10	= JBIG Color
//        99	= JPEG
//        262	= Kodak 262
//        32766	= Next
//        32767	= Sony ARW Compressed
//        32769	= Packed RAW
//        32770	= Samsung SRW Compressed
//        32771	= CCIRLEW
//        32772	= Samsung SRW Compressed 2
//        32773	= PackBits
//        32809	= Thunderscan
//        32867	= Kodak KDC Compressed
//        32895	= IT8CTPAD
//        32896	= IT8LW
//        32897	= IT8MP
//        32898	= IT8BL
//        32908	= PixarFilm
//        32909	= PixarLog
//        32946	= Deflate
//        32947	= DCS
//        33003	= Aperio JPEG 2000 YCbCr
//        33005	= Aperio JPEG 2000 RGB
//        34661	= JBIG
//        34676	= SGILog
//        34677	= SGILog24
//        34712	= JPEG 2000
//        34713	= Nikon NEF Compressed
//        34715	= JBIG2 TIFF FX
//        34718	= Microsoft Document Imaging (MDI) Binary Level Codec
//        34719	= Microsoft Document Imaging (MDI) Progressive Transform Codec
//        34720	= Microsoft Document Imaging (MDI) Vector
//        34887	= ESRI Lerc
//        34892	= Lossy JPEG
//        34925	= LZMA2
//        34926	= Zstd
//        34927	= WebP
//        34933	= PNG
//        34934	= JPEG XR
//        65000	= Kodak DCR Compressed
//        65535	= Pentax PEF Compressed
    }
    
    /**
     * * 9. Αναγνώριση ομοιογενών αντικειμένων και αυτόματη μέτρηση και για την ισορροπία χρώματος και θόρυβο
     * όπου προκύπτει αφενός ως η διαφορά μεταξύ του ελάχιστου και του μέγιστου ψηφιακού συνόλου στην τριάδα
     * υπολογιζόμενη σε σχεδόν «ουδέτερα» αντικείμενα (όπως άσφαλτος ή ταράτσες κτιρίων - δεν εφαρμόζεται σε
     * παγχρωματικές εικόνες) και αφετέρου ως η αναλογία σήματος προς θόρυβο (SNR) που καθορίζεται σαν τον
     * λόγο της μέσης ψηφιακής τιμής (DN) του pixel (DN Value) προς την μεταβλητότητα (standard deviation) των
     * ψηφιακών τιμών (υπολογισμένη σε περιοχές με ομοιόμορφη πυκνότητα μέσων τιμών) και σύμφωνα με τις
     * προδιαγραφές(*).
     *
     * @param file
     * @return
     */
    public static FileJobResult testN9(final File file, final ImagePack image) throws TikaException, IOException, SAXException {
        if (!image.isLoaded()) {
            image.loadImage();
        }
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(9);
        return resultBuilder.result(false).build();
    }
}
