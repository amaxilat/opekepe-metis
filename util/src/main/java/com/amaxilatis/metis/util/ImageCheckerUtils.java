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

import javax.imageio.IIOException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.amaxilatis.metis.config.Conditions.N1_PIXEL_SIZE;
import static com.amaxilatis.metis.config.Conditions.N2_BIT_SIZE;
import static com.amaxilatis.metis.config.Conditions.N3_SAMPLES_PER_PIXEL;
import static com.amaxilatis.metis.util.FileNameUtils.getResultFile;
import static com.amaxilatis.metis.util.WorldFileUtils.evaluateWorldFile;
import static com.amaxilatis.metis.util.WorldFileUtils.getWorldFile;
import static com.amaxilatis.metis.util.WorldFileUtils.parseWorldFile;
import static com.drew.metadata.exif.ExifDirectoryBase.TAG_BITS_PER_SAMPLE;

@Slf4j
public class ImageCheckerUtils {
    public static final ObjectMapper mapper = new ObjectMapper();
    
    public static List<FileJobResult> parseDir(final File directory, final List<Integer> tasks) throws IOException, TikaException, SAXException, ImageProcessingException {
        final List<FileJobResult> results = new ArrayList<>();
        for (final File file : Objects.requireNonNull(directory.listFiles())) {
            results.addAll(parseFile(1, file, tasks, null, null, null, null));
        }
        return results;
    }
    
    public static List<FileJobResult> parseFile(final Integer concurrency, final File file, final List<Integer> tasks, final String resultsDir, final String histogramDir, final String cloudMaskDir, final String uncompressedLocation) throws IOException, TikaException, SAXException, ImageProcessingException {
        final List<FileJobResult> results = new ArrayList<>();
        
        if (file.getName().endsWith(".tif") || file.getName().endsWith(".jpf")) {
            log.info("[{}] parsing...", file.getName());
            final ImagePack image = new ImagePack(file, cloudMaskDir, uncompressedLocation, concurrency);
            
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
                        result = ImageCheckerUtils.testN6(file, image, histogramDir);
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
            if (tasks.contains(4)) {
                try {
                    final File resultFile = getResultFile(resultsDir, file, 4);
                    final FileJobResult result;
                    if (resultsDir != null && resultFile.exists()) {
                        log.info("loading test 4 result for {}", file);
                        result = mapper.readValue(resultFile, FileJobResult.class);
                    } else {
                        log.info("running test 4 for {}", file);
                        result = ImageCheckerUtils.testN4(file, image, cloudMaskDir);
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
            
            image.cleanup();
        }
        return results;
    }
    
    /**
     * 1. Έλεγχος της χωρικής ανάλυσης όπου θα διαπιστωθεί ότι ο λόγος της τελικής ανάλυσης της ορθοαναγωγής
     * προς την απόσταση δειγματοληψίας εδάφους (απόσταση μεταξύ δύο διαδοχικών κέντρων εικονοστοιχείων
     * που μετριούνται στο έδαφος) είναι σύμφωνα με τις προδιαγραφές(*),
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN1(final File file, final ImagePack image) throws TikaException, IOException, SAXException, ImageProcessingException {
        image.loadImage();
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(1);
        
        final File worldFileFile = getWorldFile(file);
        final StringBuilder note = new StringBuilder();
        note.append("WorldFile: ");
        boolean worldRes = true;
        try {
            final WorldFile worldFile = parseWorldFile(worldFileFile);
            final WorldFileResult worldConditionRes = evaluateWorldFile(worldFile);
            log.info("[N1] file:{}, n1:{} world", file.getName(), worldConditionRes.isOk());
            note.append(worldConditionRes.getNote());
            worldRes = worldConditionRes.isOk();
        } catch (FileNotFoundException e) {
            note.append(String.format("Δεν βρέθηκε το αρχείο world της εικόνας %s", file.getName()));
            worldRes = false;
        }
        
        note.append(" | Exif: ");
        boolean metadataRes = true;
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
                note.append(String.format("Μεγέθη Χ: %f, Y: %f", doublePixelSize0, doublePixelSize1));
                log.info("[N1] file:{}, n1:{} exif", file.getName(), metadataRes);
                resultBuilder.note(note.toString());
            }
        }
        return resultBuilder.result(worldRes && metadataRes).build();
    }
    
    /**
     * 2. Έλεγχος της ραδιομετρικής ανάλυσης όπου θα επαληθευτεί ότι είναι 11-12 bits ανά κανάλι σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN2(final File file, final ImagePack image) throws IOException, ImageProcessingException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(2);
        try {
            final BufferedImage jImage = image.getImage();
            
            final ExifIFD0Directory directory = image.getIoMetadata().getFirstDirectoryOfType(ExifIFD0Directory.class);
            final String metadataValue = directory.getString(TAG_BITS_PER_SAMPLE).replaceAll("[^0-9 ]", "");
            log.info("[N2] bitPerSample {}", metadataValue);
            final String[] bitsCounts = metadataValue.split(" ");
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
            
            final String note = String.format("%d Κανάλια, Μέγεθος Pixel: %d bit, Μέγεθος/Κανάλι: %d bit | Exif Μέγεθος Pixels: %s bit", jImage.getColorModel().getNumComponents(), jImage.getColorModel().getPixelSize(), pixelSize, metadataValue);
            
            resultBuilder.note(note).result(metadataTest && (pixelSize >= N2_BIT_SIZE));
        } catch (IIOException e) {
            resultBuilder.result(false);
            resultBuilder.note(e.getMessage());
        }
        return resultBuilder.build();
    }
    
    /**
     * * 3. Έλεγχος της φασματικής ανάλυσης όπου θα διαπιστωθεί ότι το πλήθος των καναλιών είναι σύμφωνο με τα
     * στοιχεία παράδοσης και της προδιαγραφές(*),
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN3(final File file, final ImagePack image) throws IOException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(3);
        try {
            final BufferedImage jImage = image.getImage();
            
            log.debug("[N3] colorModelComponents: {}", jImage.getColorModel().getNumComponents());
            log.debug("[N3] colorModelColorComponents: {}", jImage.getColorModel().getNumColorComponents());
            log.debug("[N3] colorModelHasAlpha: {}", jImage.getColorModel().hasAlpha());
            final String note = String.format("%d Κανάλια, %d Χρώματα, Alpha: %s", jImage.getColorModel().getNumComponents(), jImage.getColorModel().getNumColorComponents(), jImage.getColorModel().hasAlpha() ? "Ναι" : "Όχι");
            boolean result = jImage.getColorModel().getNumComponents() == N3_SAMPLES_PER_PIXEL && jImage.getColorModel().getNumColorComponents() == N3_SAMPLES_PER_PIXEL - 1 && jImage.getColorModel().hasAlpha();
            resultBuilder.result(result).note(note).build();
        } catch (IIOException e) {
            resultBuilder.result(false);
            resultBuilder.note(e.getMessage());
        }
        return resultBuilder.build();
    }
    
    /**
     * * 4. Έλεγχος νεφοκάλυψης ανά εικόνα και συνολικά σε συμφωνία με τις προδιαγραφές(*),
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN4(final File file, final ImagePack image, final String cloudMaskDir) throws IOException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(4);
        try {
            image.detectClouds(true);
            double percentage = (image.getCloudPixels() / image.getValidPixels()) * 100;
            
            boolean result = percentage < 5;
            resultBuilder.result(result);
            resultBuilder.note(String.format("Εικονοστοιχεία με Σύννεφα %.0f, Συνολικά Εικονοστοιχεία %.0f, Ποσοστό: %.2f%%", image.getCloudPixels(), image.getValidPixels(), percentage));
            
            if (cloudMaskDir != null) {
                image.saveTensorflowMaskImage(new File(FileNameUtils.getImageCloudCoverMaskFilename(cloudMaskDir, file.getParentFile().getName(), file.getName())));
            }
        } catch (IIOException e) {
            resultBuilder.result(false);
            resultBuilder.note(e.getMessage());
        }
        return resultBuilder.build();
    }
    
    /**
     * * 5. Έλεγχος ολικού clipping το οποίο υπολογίζεται στο ιστόγραμμα φωτεινότητας σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN5(final File file, final ImagePack image) throws TikaException, IOException, SAXException, ImageProcessingException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(5);
        image.loadImage();
        try {
            image.loadHistogram();
            
            final double pixelsCount = image.getHistogram().getTotalPixels(ColorUtils.LAYERS.LUM);
            final Set<HistogramBin> top = image.getHistogram().getTop5Bins(ColorUtils.LAYERS.LUM);
            final Set<HistogramBin> bottom = image.getHistogram().getBottom5Bins(ColorUtils.LAYERS.LUM);
            long totalItemsInTop = top.stream().mapToLong(HistogramBin::getValuesCount).sum();
            long totalItemsInBottom = bottom.stream().mapToLong(HistogramBin::getValuesCount).sum();
            double topClipping = (totalItemsInTop / pixelsCount) * 100;
            double bottomClipping = (totalItemsInBottom / pixelsCount) * 100;
            log.info("[N5] top[{} - {}]: {}", totalItemsInTop, (totalItemsInTop / pixelsCount) * 100, top);
            log.info("[N5] bottom[{} - {}]: {}", totalItemsInBottom, (totalItemsInBottom / pixelsCount) * 100, bottom);
            
            boolean result = topClipping < 0.5 && bottomClipping < 0.5;
            resultBuilder.result(result);
            resultBuilder.note(String.format("Πρώτα: %.3f%% , Τελευταία: %.3f%%", bottomClipping, topClipping));
        } catch (IIOException e) {
            resultBuilder.result(false);
            resultBuilder.note(e.getMessage());
        }
        return resultBuilder.build();
    }
    
    /**
     * * 6. Έλεγχος κορυφής ιστογράμματος από την τυπική μέση τιμή (πχ 8bit 128) και σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN6(final File file, final ImagePack image, final String histogramDir) throws IOException, ImageProcessingException, TikaException, SAXException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(6);
        try {
            
            image.loadHistogram();
            final int centerValue = image.getHistogram().getBinCount() / 2;
            int histMinLimit = (int) (centerValue * 0.85);
            int histMaxLimit = (int) (centerValue * 1.15);
            log.info("[N6] brightness: {}< mean:{} <{} std: {}", histMinLimit, image.getHistogram().getMean(ColorUtils.LAYERS.LUM), histMaxLimit, image.getHistogram().getStandardDeviation(ColorUtils.LAYERS.LUM));
            final int majorBinCenterLum = image.getHistogram().majorBin(ColorUtils.LAYERS.LUM);
            log.info("[N6] histogramBr center: {}", majorBinCenterLum);
            
            final int majorBinCenterR = image.getHistogram().majorBin(ColorUtils.LAYERS.RED);
            log.info("[N6] histogramR center: {}", majorBinCenterR);
            final int majorBinCenterG = image.getHistogram().majorBin(ColorUtils.LAYERS.GREEN);
            log.info("[N6] histogramG center: {}", majorBinCenterG);
            final int majorBinCenterB = image.getHistogram().majorBin(ColorUtils.LAYERS.BLUE);
            log.info("[N6] histogramB center: {}", majorBinCenterB);
            
            if (histogramDir != null) {
                image.getHistogram().saveHistogramImage(new File(FileNameUtils.getImageHistogramFilename(histogramDir, file.getParentFile().getName(), file.getName())));
            }
            
            boolean result = histMinLimit < majorBinCenterLum && majorBinCenterLum < histMaxLimit;
            resultBuilder.result(result);
            resultBuilder.note(String.format("Κορυφή Ιστογράμματος: %d, όρια +/-15%%: [%d,%d], Κέντρα Ιστογράμματος Χρωμάτων: [R:%d,G:%d,B:%d]", majorBinCenterLum, histMinLimit, histMaxLimit, majorBinCenterR, majorBinCenterG, majorBinCenterB));
        } catch (IIOException e) {
            resultBuilder.result(false);
            resultBuilder.note(e.getMessage());
        }
        return resultBuilder.build();
    }
    
    /**
     * * 7. Έλεγχος αντίθεσης ανά κανάλι ως έλεγχος της μεταβλητότητας των ψηφιακών τιμών (DN) σαν ποσοστό των
     * διαθεσίμων επιπέδων του γκρι και σύμφωνα με τις προδιαγραφές(*),
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN7(final File file, final ImagePack image) throws IOException, ImageProcessingException, TikaException, SAXException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(7);
        try {
            image.loadHistogram();
            double mean = image.getDnValuesStatistics().getMean();
            double std = image.getDnValuesStatistics().getStandardDeviation();
            double coefficientOfVariation = std / mean;
            double variance = image.getDnValuesStatistics().getVariance();
            
            boolean result = coefficientOfVariation > 0.1 && coefficientOfVariation < 0.2;
            resultBuilder.result(result);
            resultBuilder.note(String.format("Μέση Τιμή: %.2f, Τυπική Απόκλιση: %.2f, Διασπορά: %.2f, Συντελεστής Διακύμανσης: %.2f", mean, std, variance, coefficientOfVariation));
        } catch (IIOException e) {
            resultBuilder.result(false);
            resultBuilder.note(e.getMessage());
        }
        
        
        return resultBuilder.build();
    }
    
    /**
     * * 8. Έλεγχος συμπίεσης στον μορφότυπο των αρχείων (GeoTiff ή/και JPEG2000) και σύμφωνα με τις
     * προδιαγραφές(*),
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN8(final File file, final ImagePack image) {
        int compressionExifValue = image.getCompressionExifValue();
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
     * λόγο της μέσης ψηφιακής τιμής (DN) του pixel (DN Value) προς τη μεταβλητότητα (standard deviation) των
     * ψηφιακών τιμών (υπολογισμένη σε περιοχές με ομοιόμορφη πυκνότητα μέσων τιμών) και σύμφωνα με τις
     * προδιαγραφές(*).
     *
     * @param file  the file containing the image to check
     * @param image an object containing details for the provided image
     * @return the result of the checks performed
     */
    public static FileJobResult testN9(final File file, final ImagePack image) throws TikaException, IOException, SAXException, ImageProcessingException {
        if (!image.isLoaded()) {
            image.loadImage();
        }
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(9);
        return resultBuilder.result(false).build();
    }
}
