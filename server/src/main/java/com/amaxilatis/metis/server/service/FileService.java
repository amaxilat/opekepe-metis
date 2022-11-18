package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.util.FileUtils;
import com.amaxilatis.metis.util.FileNameUtils;
import com.drew.lang.Charsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.amaxilatis.metis.util.FileNameUtils.getResultFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    
    public static final String FOLDER_TITLE = "Φάκελος";
    public static final String CHECKS_TITLE = "Έλεγχοι";
    public static final String FILE_TITLE = "ΑΡΧΕΙΟ";
    public static final String CHECK_TITLE = "ΕΛΕΓΧΟΣ %d";
    public static final String NOTES_TITLE = "ΠΑΡΑΤΗΡΗΣΕΙΣ %d";
    public static final String CHECK_OK = "ΟΚ";
    public static final String CHECK_NOK = "ΛΑΘΟΣ";
    private final MetisProperties props;
    
    final ExecutorService tpe = Executors.newFixedThreadPool(1);
    
    public String getResultsLocation() {
        return props.getResultsLocation();
    }
    
    public String getHistogramLocation() {
        return props.getHistogramLocation();
    }
    
    public String getCloudMaskLocation() {
        return props.getCloudMaskLocation();
    }
    
    public String getUncompressedLocation() {
        return props.getUncompressedLocation();
    }
    
    public String getFilesLocation() {
        return props.getFilesLocation();
    }
    
    public void createTempReport(final String name) {
        writeToFile(name, "", false);
    }
    
    public void append(final String name, final String text) {
        writeToFile(name, text);
    }
    
    public void append(final String name, final List<String> parts) {
        writeToFile(name, StringUtils.join(parts, ","));
    }
    
    public String csv2xlsxName(final String name) {
        return name.replace(".csv", ".xlsx");
    }
    
    public String csv2xlsx(final String name) {
        final String xlsxFileName = csv2xlsxName(name);
        final File outFile = new File(xlsxFileName);
        try (final FileOutputStream fos = new FileOutputStream(outFile)) {
            final Workbook wb = new XSSFWorkbook();
            final Sheet sheet = wb.createSheet("report");
            
            boolean first = true;
            try (CSVReader reader = new CSVReader(new FileReader(name, Charsets.UTF_8))) {
                List<String[]> r = reader.readAll();
                for (String[] strings : r) {
                    Row row;
                    if (first) {
                        row = appendRow(sheet, 1);
                        first = false;
                    } else {
                        row = appendRow(sheet);
                    }
                    Arrays.stream(strings).forEach(s -> appendCell(row, s));
                }
                r.forEach(x -> {
                
                });
            } catch (CsvException e) {
                log.error(e.getMessage(), e);
            }
            wb.write(fos);
            wb.close();
            return xlsxFileName;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            boolean result = outFile.delete();
            log.error("delete outFile {} {}", outFile.getName(), result);
        }
        return null;
    }
    
    private void writeToFile(final String name, final String text) {
        writeToFile(name, text, true);
    }
    
    @Synchronized
    private void writeToFile(final String name, final String text, final boolean append) {
        try {
            final FileWriter myWriter = new FileWriter(name, Charsets.UTF_8, append);
            myWriter.write(text + "\n");
            myWriter.flush();
            myWriter.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    private static Row appendRow(final Sheet sheet) {
        return appendRow(sheet, 1);
    }
    
    private static Row appendRow(final Sheet sheet, final int rowOffset) {
        return sheet.createRow(sheet.getLastRowNum() + rowOffset);
    }
    
    private static void appendCell(final Row row, final List<String> text) {
        text.forEach(s -> appendCell(row, s));
    }
    
    private static void appendCell(final Row row, final String text) {
        final Cell cell = row.createCell(row.getLastCellNum() == -1 ? 0 : row.getLastCellNum());
        cell.setCellValue(text);
    }
    
    final Map<String, SortedSet<ImageFileInfo>> images = new HashMap<>();
    final SortedSet<ImageFileInfo> imagesDirs = new TreeSet<>();
    final ObjectMapper mapper = new ObjectMapper();
    
    @PostConstruct
    public void init() {
        checkAndCreateDirectory(props.getReportLocation());
        checkAndCreateDirectory(props.getResultsLocation());
        checkAndCreateDirectory(props.getThumbnailLocation());
        checkAndCreateDirectory(props.getHistogramLocation());
        checkAndCreateDirectory(props.getCloudMaskLocation());
        checkAndCreateDirectory(props.getUncompressedLocation());
        updateImageDirs(true);
    }
    
    /**
     * Checks if the required directory exists and creates it if needed.
     *
     * @param location the directory to create
     * @return true if the directory exists or is created, false if there was an error.
     */
    private boolean checkAndCreateDirectory(final String location) {
        final File locationDir = new File(location);
        if (!locationDir.exists()) {
            log.info("creating {} directory...", locationDir.getName());
            boolean result = locationDir.mkdirs();
            log.debug("created {} directory {}", locationDir.getName(), result);
            return result;
        } else {
            return true;
        }
    }
    
    /**
     * Updates the list of available image directories and image files.
     *
     * @param generateThumbnails flag to generate or not thumbnails for the detected images.
     */
    public void updateImageDirs(final boolean generateThumbnails) {
        long start = System.currentTimeMillis();
        imagesDirs.clear();
        images.clear();
        log.debug("[updateImageDirs] filesLocation[{}]: {}", new File(props.getFilesLocation()).exists(), props.getFilesLocation());
        final File[] imageDirectoryList = new File(props.getFilesLocation()).listFiles(File::isDirectory);
        log.debug("[updateImageDirs] imageDirectoryList: {}", Arrays.toString(imageDirectoryList));
        if (imageDirectoryList != null) {
            Arrays.stream(imageDirectoryList).forEach(imagesDirectory -> {
                final String imagesDirectoryName = imagesDirectory.getName();
                log.debug("[updateImageDirs] imagesDirectory: {}", imagesDirectoryName);
                images.put(imagesDirectoryName, new TreeSet<>());
                final File[] filesList = imagesDirectory.listFiles((dir, name) -> StringUtils.endsWithAny(name.toLowerCase(), ".tif", "jp2"));
                if (filesList != null) {
                    final Set<ImageFileInfo> imageSet = Arrays.stream(filesList).map(image -> {
                        final String imageName = image.getName();
                        if (generateThumbnails) {
                            tpe.execute(() -> getImageThumbnail(imagesDirectoryName, imageName));
                        }
                        log.trace("[updateImageDirs] imagesDirectory: {} image: {}", imagesDirectoryName, imageName);
                        return ImageFileInfo.builder().name(imageName).hash(getStringHash(imageName)).build();
                    }).collect(Collectors.toSet());
                    if (!imageSet.isEmpty()) {
                        images.get(imagesDirectoryName).addAll(imageSet);
                        imagesDirs.add(ImageFileInfo.builder().name(imagesDirectoryName).hash(getStringHash(imagesDirectoryName)).count(imageSet.size()).build());
                        checkAndCreateDirectory(props.getResultsLocation() + "/" + imagesDirectoryName);
                        checkAndCreateDirectory(props.getThumbnailLocation() + "/" + imagesDirectoryName);
                        checkAndCreateDirectory(props.getHistogramLocation() + "/" + imagesDirectoryName);
                        checkAndCreateDirectory(props.getCloudMaskLocation() + "/" + imagesDirectoryName);
                        checkAndCreateDirectory(props.getUncompressedLocation() + "/" + imagesDirectoryName);
                    }
                }
            });
            
            //cleanup results from deleted files
            Arrays.stream(imageDirectoryList).forEach(imagesDirectory -> {
                cleanupDirectoryResults(imagesDirectory);
            });
        } else {
            log.warn("[updateImageDirs] imageDirectoryList is null!!!");
        }
        log.info("[updateImageDirs] time:{}", (System.currentTimeMillis() - start));
    }
    
    /**
     * Cleans old result files from the specified directory.
     *
     * @param imagesDirectory the directory contains the images results.
     */
    private void cleanupDirectoryResults(final File imagesDirectory) {
        final String imagesDirectoryName = imagesDirectory.getName();
        final File files = new File(props.getResultsLocation(), imagesDirectoryName);
        final File[] flist = files.listFiles();
        if (flist != null) {
            Arrays.stream(flist).forEach(file -> {
                if (file.getName().endsWith(".result")) {
                    final String[] parts = file.getName().split("\\.");
                    final String filename = parts[0] + "." + parts[1];
                    if (!new File(new File(props.getFilesLocation(), imagesDirectoryName), filename).exists()) {
                        final boolean deleted = file.delete();
                        log.warn("removed[{}] old result[{}] for file {}", deleted, file.getName(), filename);
                    }
                }
            });
        }
    }
    
    
    @Scheduled(fixedRate = 600000L)
    public void runAllImages() {
        images.forEach((directoryName, value) -> value.forEach(imageFileInfo -> {
            if (!Files.exists(Paths.get(getImageThumbnailFilename(directoryName, imageFileInfo.getName())))) {
                tpe.execute(() -> getImageThumbnail(directoryName, imageFileInfo.getName()));
            }
        }));
    }
    
    public SortedSet<ImageFileInfo> getImagesDirs() {
        return imagesDirs;
    }
    
    public SortedSet<ImageFileInfo> listImages(final String directory) {
        return images.get(directory);
    }
    
    public String getStringHash(final String name) {
        //return Base64.encode(name).replaceAll("=", "-");
        //return DigestUtils.md5Hex(name);
        return URLEncoder.encode(name, Charsets.UTF_8);
    }
    
    public String getStringFromHash(final String hash) {
        //return Base64.decode(hash.replaceAll("-", "="));
        return URLDecoder.decode(hash, Charsets.UTF_8);
    }
    
    public void clean(final String directory, final List<Integer> tasks) {
        File filesDir = new File(props.getFilesLocation());
        File filesSubDir = new File(filesDir, directory);
        final List<File> fileList = new ArrayList<>();
        Arrays.stream(Objects.requireNonNull(filesSubDir.listFiles())).filter(file -> file.getName().endsWith(".tif")).forEach(fileList::add);
        tasks.forEach(task -> fileList.forEach(file -> cleanFileResults(directory, file, task)));
    }
    
    private void cleanFileResults(final String directory, final File file, final int task) {
        File resultsDir = new File(props.getResultsLocation(), directory);
        new File(resultsDir, FileNameUtils.getResultName(file, task)).delete();
        if (task == 4) {
            new File(FileNameUtils.getImageCloudCoverMaskFilename(props.getCloudMaskLocation(), file.getParentFile().getName(), file.getName())).delete();
        }
        if (task == 5) {
            new File(FileNameUtils.getImageHistogramFilename(props.getHistogramLocation(), file.getParentFile().getName(), file.getName())).delete();
        }
        if (task == 9) {
            new File(FileNameUtils.getImageColorBalanceMaskFilename(props.getHistogramLocation(), file.getParentFile().getName(), file.getName())).delete();
        }
    }
    
    public List<FileJobResult> getImageResults(final String decodedImageDir, final String decodedImage) {
        File resultsDir = new File(props.getResultsLocation(), decodedImageDir);
        File image = new File(resultsDir, decodedImage);
        List<FileJobResult> results = new ArrayList<>();
        for (int task = 0; task < 10; task++) {
            File f = new File(resultsDir, FileNameUtils.getResultName(image, task));
            if (f.exists()) {
                try {
                    results.add(parseResult(f));
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return results;
    }
    
    private FileJobResult parseResult(final File f) throws IOException {
        return mapper.readValue(f, FileJobResult.class);
    }
    
    @Async
    public void deleteFile(final String resultFile) {
        try {
            Thread.sleep(30000);
            Files.deleteIfExists(Path.of(resultFile));
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
    
    
    /**
     * Returns the full path to the image's file.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's file.
     */
    String getImageFilename(final String dir, final String name) {
        return FileNameUtils.getImageFilename(props.getFilesLocation(), dir, name);
    }
    
    /**
     * Returns the full path to the image's thumbnail.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's thumbnail file.
     */
    String getImageThumbnailFilename(final String dir, final String name) {
        return FileNameUtils.getImageThumbnailFilename(props.getThumbnailLocation(), dir, name);
    }
    
    /**
     * Returns the full path to the image's histogram.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's histogram file.
     */
    String getImageHistogramFilename(final String dir, final String name) {
        return FileNameUtils.getImageHistogramFilename(props.getHistogramLocation(), dir, name);
    }
    
    /**
     * Returns the full path to the image's color balance mask.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's color balance mask file.
     */
    String getImageColorBalanceFilename(final String dir, final String name) {
        return FileNameUtils.getImageColorBalanceMaskFilename(props.getHistogramLocation(), dir, name);
    }
    
    /**
     * Returns the full path to the image's cloud coverage mask.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's cloud coverage mask file.
     */
    String getImageCloudCoverFilename(final String dir, final String name) {
        return FileNameUtils.getImageCloudCoverMaskFilename(props.getCloudMaskLocation(), dir, name);
    }
    
    /**
     * Get or create and get the thumbnail of the image.
     *
     * @param directory the directory of the image.
     * @param name      the name of the image.
     * @return the file containing the thumbnail of the image.
     */
    public File getImageThumbnail(final String directory, final String name) {
        final File thumbnailFile = new File(getImageThumbnailFilename(directory, name));
        log.debug("[thumbnail|{}|{}] {}", directory, name, thumbnailFile.getAbsolutePath());
        //check if directory exists
        if (!thumbnailFile.getParentFile().exists()) {
            thumbnailFile.getParentFile().mkdir();
        }
        //check if file exists
        if (thumbnailFile.exists()) {
            return thumbnailFile;
        } else {
            long start = System.currentTimeMillis();
            try {
                FileUtils.makeThumbnail(new File(getImageFilename(directory, name)), thumbnailFile, 450, 339);
                log.info("[thumbnail-create|{}|{}] time:{}", directory, name, (System.currentTimeMillis() - start));
                return thumbnailFile;
            } catch (IOException e) {
                return null;
            }
        }
    }
    
    /**
     * Get or create and get the histogram of the image.
     *
     * @param directory the directory of the image.
     * @param name      the name of the image.
     * @return the file containing the histogram of the image.
     */
    public File getImageHistogram(final String directory, final String name) {
        final File histogramFile = new File(getImageHistogramFilename(directory, name));
        log.debug("[histogram|{}|{}] {}", directory, name, histogramFile.getAbsolutePath());
        //check if directory exists
        if (!histogramFile.getParentFile().exists()) {
            histogramFile.getParentFile().mkdir();
        }
        //check if file exists
        if (histogramFile.exists()) {
            return histogramFile;
        } else {
            return null;
        }
    }
    
    /**
     * Get or create and get the color balance of the image.
     *
     * @param directory the directory of the image.
     * @param name      the name of the image.
     * @return the file containing the color balance of the image.
     */
    public File getImageColorBalance(final String directory, final String name) {
        final File colorBalanceFile = new File(getImageColorBalanceFilename(directory, name));
        log.debug("[colorBalance|{}|{}] {}", directory, name, colorBalanceFile.getAbsolutePath());
        //check if directory exists
        if (!colorBalanceFile.getParentFile().exists()) {
            colorBalanceFile.getParentFile().mkdir();
        }
        //check if file exists
        if (colorBalanceFile.exists()) {
            return colorBalanceFile;
        } else {
            return null;
        }
    }
    
    /**
     * Get or create and get the cloud coverage mask of the image.
     *
     * @param directory the directory of the image.
     * @param name      the name of the image.
     * @return the file containing the cloud coverage mask of the image.
     */
    public File getImageCloudCover(final String directory, final String name) {
        final File cloudCoverFile = new File(getImageCloudCoverFilename(directory, name));
        log.debug("[cloudCover|{}|{}] {}", directory, name, cloudCoverFile.getAbsolutePath());
        //check if directory exists
        if (!cloudCoverFile.getParentFile().exists()) {
            cloudCoverFile.getParentFile().mkdir();
        }
        //check if file exists
        if (cloudCoverFile.exists()) {
            return cloudCoverFile;
        } else {
            return null;
        }
    }
    
    public File generateDirectoryReportXlsx(final String name) {
        final String xlsxFileName = "report_metis-" + name + "-" + System.currentTimeMillis() + ".xlsx";
        final File outFile = new File(props.getReportLocation(), xlsxFileName);
        try (final FileOutputStream fos = new FileOutputStream(outFile)) {
            final Workbook wb = new XSSFWorkbook();
            final Sheet sheet = wb.createSheet("report");
            
            final Row folderRow = appendRow(sheet, 1);
            appendCell(folderRow, FOLDER_TITLE);
            appendCell(folderRow, name);
            
            final Row checksRow = appendRow(sheet, 1);
            appendCell(checksRow, CHECKS_TITLE);
            appendCell(checksRow, "1-2-3-4-5-6-7-8-9");
            
            final Row titleRow = appendRow(sheet, 1);
            appendCell(titleRow, FILE_TITLE);
            for (int i = 1; i < 10; i++) {
                appendCell(titleRow, String.format(CHECK_TITLE, i));
            }
            for (int i = 1; i < 10; i++) {
                appendCell(titleRow, String.format(NOTES_TITLE, i));
            }
            
            final File[] fileList = new File(props.getResultsLocation(), name).listFiles();
            if (fileList != null) {
                Arrays.stream(fileList) //for all files
                        .map(File::getName) //extract name
                        .map(FileNameUtils::extractImageNameFromResult) //extract image name
                        .distinct() //unique
                        .forEach(filename -> {
                            final Row fileRow = appendRow(sheet, 1);
                            appendCell(fileRow, filename);
                            for (int i = 1; i < 10; i++) {
                                final File resultFile = getResultFile(props.getResultsLocation(), new File(props.getFilesLocation() + "/" + name + "/", filename), i);
                                final FileJobResult result;
                                if (resultFile.exists()) {
                                    try {
                                        result = mapper.readValue(resultFile, FileJobResult.class);
                                        appendCell(fileRow, result.getResult() ? CHECK_OK : CHECK_NOK);
                                    } catch (IOException e) {
                                        appendCell(fileRow, "");
                                    }
                                } else {
                                    appendCell(fileRow, "");
                                }
                            }
                            for (int i = 1; i < 10; i++) {
                                final File resultFile = getResultFile(props.getResultsLocation(), new File(props.getFilesLocation() + "/" + name + "/", filename), i);
                                final FileJobResult result;
                                if (resultFile.exists()) {
                                    try {
                                        result = mapper.readValue(resultFile, FileJobResult.class);
                                        appendCell(fileRow, result.getNote());
                                    } catch (IOException e) {
                                        appendCell(fileRow, "");
                                    }
                                } else {
                                    appendCell(fileRow, "");
                                }
                            }
                        });
            }
            
            wb.write(fos);
            wb.close();
            return outFile;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
