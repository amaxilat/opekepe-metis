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

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final MetisProperties props;
    
    ExecutorService tpe = Executors.newFixedThreadPool(1);
    
    public String getResultsLocation() {
        return props.getResultsLocation();
    }
    
    public String getHistogramLocation() {
        return props.getHistogramLocation();
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
    
    @PostConstruct
    public void init() {
        
        final File reports = new File(props.getReportLocation());
        if (!reports.exists()) {
            log.info("creating reports directory...");
            boolean result = reports.mkdirs();
            log.debug("created reports directory {}", result);
        }
        final File thumbs = new File(props.getThumbnailLocation());
        if (!thumbs.exists()) {
            log.info("creating thumbnail directory...");
            boolean result = thumbs.mkdirs();
            log.debug("created thumbnail directory {}", result);
        }
        final File hists = new File(props.getHistogramLocation());
        if (!hists.exists()) {
            log.info("creating histogram directory...");
            boolean result = hists.mkdirs();
            log.debug("created histogram directory {}", result);
        }
        updateImageDirs(true);
    }
    
    public void updateImageDirs(final boolean generateThumbnails) {
        long start = System.currentTimeMillis();
        imagesDirs.clear();
        images.clear();
        log.debug("[updateImageDirs] filesLocation: {}", props.getFilesLocation());
        final File[] imageDirectoryList = new File(props.getFilesLocation()).listFiles(File::isDirectory);
        log.debug("[updateImageDirs] imageDirectoryList: {}", Arrays.toString(imageDirectoryList));
        if (imageDirectoryList != null) {
            Arrays.stream(imageDirectoryList).forEach(imagesDirectory -> {
                final String imagesDirectoryName = imagesDirectory.getName();
                log.trace("[updateImageDirs] imagesDirectory: {}", imagesDirectoryName);
                images.put(imagesDirectoryName, new TreeSet<>());
                final File[] filesList = imagesDirectory.listFiles((dir, name) -> StringUtils.endsWithAny(name.toLowerCase(), ".tif", "jpf"));
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
                    }
                }
            });
        }
        log.info("[updateImageDirs] time:{}", (System.currentTimeMillis() - start));
    }
    
    
    @Scheduled(fixedRate = 600000L)
    public void runAllImages() {
        for (final Map.Entry<String, SortedSet<ImageFileInfo>> stringSortedSetEntry : images.entrySet()) {
            for (final ImageFileInfo imageFileInfo : stringSortedSetEntry.getValue()) {
                final File thumbnailFile = new File(getImageThumbnailFilename(stringSortedSetEntry.getKey(), imageFileInfo.getName()));
                if (!thumbnailFile.exists()) {
                    tpe.execute(() -> getImageThumbnail(stringSortedSetEntry.getKey(), imageFileInfo.getName()));
                }
            }
        }
    }
    
    public SortedSet<ImageFileInfo> getImagesDirs() {
        return imagesDirs;
    }
    
    public SortedSet<ImageFileInfo> listImages(final String directory) {
        return images.get(directory);
    }
    
    public String getStringHash(String name) {
        //return Base64.encode(name).replaceAll("=", "-");
        //return DigestUtils.md5Hex(name);
        return URLEncoder.encode(name, Charsets.UTF_8);
    }
    
    public String getStringFromHash(String hash) {
        //return Base64.decode(hash.replaceAll("-", "="));
        return URLDecoder.decode(hash, Charsets.UTF_8);
    }
    
    public void clean(String directory, List<Integer> tasks) {
        File filesDir = new File(props.getFilesLocation());
        File filesSubDir = new File(filesDir, directory);
        final List<File> fileList = new ArrayList<>();
        Arrays.stream(Objects.requireNonNull(filesSubDir.listFiles())).filter(file -> file.getName().endsWith(".tif")).forEach(fileList::add);
        for (final File file : fileList) {
            cleanFileResults(directory, file);
        }
    }
    
    private void cleanFileResults(String directory, File file) {
        File resultsDir = new File(props.getResultsLocation(), directory);
        for (int task = 0; task < 10; task++) {
            File f = new File(resultsDir, FileNameUtils.getResultName(file, task));
            f.delete();
        }
    }
    
    public List<FileJobResult> getImageResults(String decodedImageDir, String decodedImage) {
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
    
    private FileJobResult parseResult(File f) throws IOException {
        return new ObjectMapper().readValue(f, FileJobResult.class);
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
     * Returns the full path to the image's cloud coverage mask.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's cloud coverage mask file.
     */
    String getImageCloudCoverFilename(final String dir, final String name) {
        return FileNameUtils.getImageCloudCoverMaskFilename(props.getHistogramLocation(), dir, name);
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
}
