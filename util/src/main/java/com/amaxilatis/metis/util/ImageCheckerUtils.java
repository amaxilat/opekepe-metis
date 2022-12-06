package com.amaxilatis.metis.util;

import com.amaxilatis.metis.model.ActionNote;
import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.model.HistogramBin;
import com.amaxilatis.metis.model.ImagePack;
import com.amaxilatis.metis.model.TestConfiguration;
import com.amaxilatis.metis.model.WorldFile;
import com.amaxilatis.metis.model.WorldFileResult;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import javax.imageio.IIOException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.amaxilatis.metis.util.FileNameUtils.getResultFile;
import static com.amaxilatis.metis.util.WorldFileUtils.evaluateWorldFile;
import static com.amaxilatis.metis.util.WorldFileUtils.getWorldFile;
import static com.amaxilatis.metis.util.WorldFileUtils.parseWorldFile;
import static com.drew.metadata.exif.ExifDirectoryBase.TAG_BITS_PER_SAMPLE;

@Slf4j
public class ImageCheckerUtils {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @Getter
    private static final Deque<ActionNote> actionNotes = new ConcurrentLinkedDeque<>();
    
    public static List<FileJobResult> parseDir(final TestConfiguration configuration, final File directory, final List<Integer> tasks) throws IOException, TikaException, SAXException, ImageProcessingException {
        final List<FileJobResult> results = new ArrayList<>();
        for (final File file : Objects.requireNonNull(directory.listFiles())) {
            results.addAll(parseFile(configuration, 1, file, tasks, null, null, null, null));
        }
        return results;
    }
    
    public static List<FileJobResult> parseFile(final TestConfiguration configuration, final Integer concurrency, final File file, final List<Integer> tasks, final String resultsDir, final String histogramDir, final String cloudMaskDir, final String uncompressedLocation) throws IOException, TikaException, SAXException, ImageProcessingException {
        final List<FileJobResult> results = new ArrayList<>();
        
        if (file.getName().endsWith(".tif") || file.getName().endsWith(".jpf")) {
            log.info("[{}] parsing...", file.getName());
            ImagePack image = null;
            int[] orderedTests = new int[]{8, 1, 2, 3, 5, 6, 7, 4, 9};
            for (int orderedTest : orderedTests) {
                final Pair<ImagePack, FileJobResult> pair = tryRunTest(configuration, orderedTest, tasks, image, resultsDir, file, cloudMaskDir, uncompressedLocation, histogramDir, concurrency);
                image = pair.getLeft();
                if (pair.getRight() != null) {
                    results.add(pair.getRight());
                }
            }
            if (image != null) {
                image.cleanup();
            }
        }
        return results;
    }
    
    private static Pair<ImagePack, FileJobResult> tryRunTest(final TestConfiguration configuration, final int test, final List<Integer> tasks, ImagePack image, final String resultsDir, final File file, final String cloudMaskDir, final String uncompressedLocation, final String histogramDir, final Integer concurrency) {
        if (tasks.contains(test)) {
            try {
                final File resultFile = getResultFile(resultsDir, file, test);
                FileJobResult result = null;
                if (resultsDir != null && resultFile.exists()) {
                    note(test, file.getParentFile().getName(), file.getName(), true, null, null);
                    log.info("loading test {} result for {}", test, file);
                    result = mapper.readValue(resultFile, FileJobResult.class);
                    note(test, file.getParentFile().getName(), file.getName(), false, result.getResult(), null);
                } else {
                    if (image == null) {
                        final long start = System.currentTimeMillis();
                        note(0, file.getParentFile().getName(), file.getName(), true, null, null);
                        image = loadImage(file, cloudMaskDir, uncompressedLocation, concurrency);
                        note(0, file.getParentFile().getName(), file.getName(), false, true, System.currentTimeMillis() - start);
                    }
                    
                    log.info("running test {} for {}", test, file);
                    note(test, file.getParentFile().getName(), file.getName(), true, null, null);
                    final long start = System.currentTimeMillis();
                    if (test == 1) {
                        result = testN1(file, image, configuration);
                    } else if (test == 2) {
                        result = testN2(file, image, configuration);
                    } else if (test == 3) {
                        result = testN3(file, image, configuration);
                    } else if (test == 4) {
                        result = testN4(file, image, cloudMaskDir, configuration);
                    } else if (test == 5) {
                        result = testN5(file, image, configuration);
                    } else if (test == 6) {
                        result = testN6(file, image, histogramDir, configuration);
                    } else if (test == 7) {
                        result = testN7(file, image, configuration);
                    } else if (test == 8) {
                        result = testN8(file, image, configuration);
                    } else if (test == 9) {
                        result = testN9(file, image, histogramDir, configuration);
                    }
                    note(test, file.getParentFile().getName(), file.getName(), false, result.getResult(), System.currentTimeMillis() - start);
                    if (resultsDir != null) {
                        mapper.writeValue(resultFile, result);
                    }
                }
                return new ImmutablePair<>(image, result);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return new ImmutablePair<>(image, null);
    }
    
    /**
     * 1. Έλεγχος της χωρικής ανάλυσης όπου θα διαπιστωθεί ότι ο λόγος της τελικής ανάλυσης της ορθοαναγωγής
     * προς την απόσταση δειγματοληψίας εδάφους (απόσταση μεταξύ δύο διαδοχικών κέντρων εικονοστοιχείων
     * που μετριούνται στο έδαφος) είναι σύμφωνα με τις προδιαγραφές(*),
     *
     * @param file          the file containing the image to check
     * @param image         an object containing details for the provided image
     * @param configuration
     * @return the result of the checks performed
     */
    public static FileJobResult testN1(final File file, final ImagePack image, final TestConfiguration configuration) throws TikaException, IOException, SAXException, ImageProcessingException {
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
            resultBuilder.n1XPixelSizeWorld(worldFile.getXPixelSize());
            resultBuilder.n1YPixelSizeWorld(worldFile.getYPixelSize());
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
                double doublePixelSize1 = Double.parseDouble(pixelSizes[1]);
                if (doublePixelSize0 != configuration.getN1PixelSize() || doublePixelSize1 != configuration.getN1PixelSize()) {
                    metadataRes = false;
                }
                resultBuilder.n1XPixelSize(doublePixelSize0);
                resultBuilder.n1YPixelSize(doublePixelSize1);
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
    public static FileJobResult testN2(final File file, final ImagePack image, final TestConfiguration configuration) throws IOException, ImageProcessingException {
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
                metadataTest = bitsCountInt == configuration.getN2BitSize();
            }
            
            log.debug("[N2] colorModelComponents: {}", jImage.getColorModel().getNumComponents());
            log.debug("[N2] colorModelPixelSize: {}", jImage.getColorModel().getPixelSize());
            final int pixelSize = jImage.getColorModel().getPixelSize() / jImage.getColorModel().getNumComponents();
            log.debug("[N2] bitPerPixel: {}", pixelSize);
            
            final String note = String.format("%d Κανάλια, Μέγεθος Pixel: %d bit, Μέγεθος/Κανάλι: %d bit | Exif Μέγεθος Pixels: %s bit", jImage.getColorModel().getNumComponents(), jImage.getColorModel().getPixelSize(), pixelSize, metadataValue);
            
            resultBuilder.note(note).result(metadataTest && (pixelSize == configuration.getN2BitSize())).n2BitSize(pixelSize);
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
    public static FileJobResult testN3(final File file, final ImagePack image, final TestConfiguration configuration) throws IOException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(3);
        try {
            final BufferedImage jImage = image.getImage();
            
            log.debug("[N3] colorModelComponents: {}", jImage.getColorModel().getNumComponents());
            log.debug("[N3] colorModelColorComponents: {}", jImage.getColorModel().getNumColorComponents());
            log.debug("[N3] colorModelHasAlpha: {}", jImage.getColorModel().hasAlpha());
            final String note = String.format("%d Κανάλια, %d Χρώματα, Alpha: %s", jImage.getColorModel().getNumComponents(), jImage.getColorModel().getNumColorComponents(), jImage.getColorModel().hasAlpha() ? "Ναι" : "Όχι");
            boolean result = jImage.getColorModel().getNumComponents() == configuration.getN3SamplesPerPixel()
                    //color
                    && jImage.getColorModel().getNumColorComponents() == configuration.getN3SamplesPerPixel() - 1
                    //alpha
                    && jImage.getColorModel().hasAlpha();
            resultBuilder.result(result).note(note)
                    //samplesPerPixel
                    .n3SamplesPerPixel(jImage.getColorModel().getNumComponents())
                    //color
                    .n3SamplesPerPixelColor(jImage.getColorModel().getNumColorComponents())
                    //alpha
                    .n3HasAlpha(jImage.getColorModel().hasAlpha()).build();
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
    public static FileJobResult testN4(final File file, final ImagePack image, final String cloudMaskDir, final TestConfiguration configuration) throws IOException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(4);
        try {
            image.detectClouds(true);
            double percentage = (image.getCloudPixels() / image.getValidPixels()) * 100;
            
            boolean result = percentage < configuration.getN4CloudCoverageThreshold();
            resultBuilder.result(result);
            resultBuilder.n4CloudCoverage(percentage);
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
    public static FileJobResult testN5(final File file, final ImagePack image, final TestConfiguration configuration) throws TikaException, IOException, SAXException, ImageProcessingException {
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
            
            boolean result = topClipping < configuration.getN5ClippingThreshold() && bottomClipping < configuration.getN5ClippingThreshold();
            resultBuilder.b5TopClipping(topClipping).n5BottomClipping(bottomClipping);
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
     * @param file         the file containing the image to check
     * @param image        an object containing details for the provided image
     * @param histogramDir the directory where histogram images are stored
     * @return the result of the checks performed
     */
    public static FileJobResult testN6(final File file, final ImagePack image, final String histogramDir, final TestConfiguration configuration) throws IOException, ImageProcessingException, TikaException, SAXException {
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
            
            log.info("[N6] histogram dir {}", histogramDir);
            if (histogramDir != null) {
                image.getHistogram().saveHistogramImage(new File(FileNameUtils.getImageHistogramFilename(histogramDir, file.getParentFile().getName(), file.getName())));
            }
            
            boolean result = histMinLimit < majorBinCenterLum && majorBinCenterLum < histMaxLimit;
            resultBuilder.n6LumHistCenter(majorBinCenterLum);
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
    public static FileJobResult testN7(final File file, final ImagePack image, final TestConfiguration configuration) throws IOException, ImageProcessingException, TikaException, SAXException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(7);
        try {
            image.loadHistogram();
            final double mean = image.getDnValuesStatistics().getMean();
            final double std = image.getDnValuesStatistics().getStandardDeviation();
            final double coefficientOfVariation = (std / mean) * 100;
            final double variance = image.getDnValuesStatistics().getVariance();
    
            final boolean result = coefficientOfVariation >= configuration.getN7VariationLow() && coefficientOfVariation <= configuration.getN7VariationHigh();
            resultBuilder.n7CoefficientOfVariation(coefficientOfVariation);
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
    public static FileJobResult testN8(final File file, final ImagePack image, final TestConfiguration configuration) {
        int compressionExifValue = image.getCompressionExifValue();
        log.info("[N8] compressionExifValue: {}", compressionExifValue);
        
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(8);
        
        if (file.getName().endsWith(".tif")) {
            resultBuilder.note("Συμπίεση: " + CompressionUtils.toText(compressionExifValue));
            resultBuilder.n8Compression(CompressionUtils.toText(compressionExifValue));
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
     * @param file         the file containing the image to check
     * @param image        an object containing details for the provided image
     * @param histogramDir the directory where histogram images are stored
     * @return the result of the checks performed
     */
    public static FileJobResult testN9(final File file, final ImagePack image, final String histogramDir, final TestConfiguration configuration) throws TikaException, IOException, SAXException, ImageProcessingException {
        final FileJobResult.FileJobResultBuilder resultBuilder = FileJobResult.builder().name(file.getName()).task(9);
        try {
            image.loadColorBalance();
            final double std = image.getColorBalanceStatistics().getStandardDeviation();
            
            log.info("[N9] histogram dir {}", histogramDir);
            if (histogramDir != null) {
                image.saveColorBalanceMaskImage(new File(FileNameUtils.getImageColorBalanceMaskFilename(histogramDir, file.getParentFile().getName(), file.getName())));
            }
            
            boolean result = std < configuration.getN9ColorBalanceThreshold() && image.getRedSnr() > configuration.getN9NoiseThreshold() && image.getGreenSrn() > configuration.getN9NoiseThreshold() && image.getBlueSnr() > configuration.getN9NoiseThreshold();
            resultBuilder.n9ColorBalance(std).n9RedSnr(image.getRedSnr()).n9GreenSnr(image.getGreenSrn()).n9BlueSnr(image.getBlueSnr());
            resultBuilder.result(result);
            resultBuilder.note(String.format("Ισορροπία Χρώματος Τυπική Απόκλιση: %.2f, Θόρυβος: R: %.2f G: %.2f B: %.2f", std, image.getRedSnr(), image.getGreenSrn(), image.getBlueSnr()));
        } catch (IIOException e) {
            resultBuilder.result(false);
            resultBuilder.note(e.getMessage());
        }
        return resultBuilder.build();
    }
    
    private static ImagePack loadImage(final File imageFile, final String cloudMaskDir, final String uncompressedLocation, final Integer concurrency) throws ImageProcessingException, IOException {
        return new ImagePack(imageFile, cloudMaskDir, uncompressedLocation, concurrency);
    }
    
    private static void note(final int testId, final String dirName, final String fileName, final boolean start, final Boolean result, final Long time) {
        if (actionNotes.size() > 1000) {
            actionNotes.removeLast();
        }
        actionNotes.addFirst(new ActionNote(dirName, fileName, testId, new Date(), start, result, time));
    }
}
