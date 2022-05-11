package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.util.FileUtils;
import com.drew.lang.Charsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import ij.ImagePlus;
import ij.io.FileSaver;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.amaxilatis.metis.util.FileUtils.getResultName;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final MetisProperties props;
    
    public String getResultsLocation() {
        return props.getResultsLocation();
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
        updateImageDirs();
    }
    
    public void updateImageDirs() {
        long start = System.currentTimeMillis();
        imagesDirs.clear();
        images.clear();
        log.info("[updateImageDirs] filesLocation: {}", props.getFilesLocation());
        File[] imageDirectoryList = new File(props.getFilesLocation()).listFiles(File::isDirectory);
        log.info("[updateImageDirs] imageDirectoryList: {}", imageDirectoryList);
        imageDirectoryList = new File(props.getFilesLocation()).listFiles();
        log.info("[updateImageDirs] imageDirectoryList: {}", imageDirectoryList);
        if (imageDirectoryList != null) {
            for (final File imagesDirectory : imageDirectoryList) {
                final String imagesDirectoryName = imagesDirectory.getName();
                log.info("[updateImageDirs] imagesDirectory: {}", imagesDirectoryName);
                images.put(imagesDirectoryName, new TreeSet<>());
                final Set<ImageFileInfo> imageSet = new HashSet<>();
                long count = 0;
                final File[] filesList = imagesDirectory.listFiles(File::isFile);
                if (filesList != null) {
                    for (final File image : filesList) {
                        final String imageName = image.getName();
                        if (imageName.endsWith(".tif")) {
                            log.info("[updateImageDirs] imagesDirectory: {} image: {}", imagesDirectoryName, imageName);
                            count++;
                            imageSet.add(ImageFileInfo.builder().name(imageName).hash(getStringHash(imageName)).build());
                        }
                    }
                }
                if (count > 0) {
                    images.get(imagesDirectoryName).addAll(imageSet);
                    imagesDirs.add(ImageFileInfo.builder().name(imagesDirectoryName).hash(getStringHash(imagesDirectoryName)).count(count).build());
                }
            }
        }
        log.info("updateImageDirs took {}ms", (System.currentTimeMillis() - start));
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
            File f = new File(resultsDir, getResultName(file, task));
            f.delete();
        }
    }
    
    public List<FileJobResult> getImageResults(String decodedImageDir, String decodedImage) {
        File resultsDir = new File(props.getResultsLocation(), decodedImageDir);
        File image = new File(resultsDir, decodedImage);
        List<FileJobResult> results = new ArrayList<>();
        for (int task = 0; task < 10; task++) {
            File f = new File(resultsDir, getResultName(image, task));
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
    
    public File getImageThumbnail(final String decodedImageDir, final String decodedImage) {
        log.info("[thumb] " + props.getThumbnailLocation() + "/" + decodedImageDir + "/" + decodedImage);
        final File thumbnailFile = new File(props.getThumbnailLocation() + "/" + decodedImage + ".jpg");
        if (thumbnailFile.exists()) {
            return thumbnailFile;
        } else {
            log.info("[thumb:1] " + thumbnailFile.getAbsolutePath());
            final long start = System.currentTimeMillis();
            final ImagePlus ip = FileUtils.makeThumbnail(new ImagePlus(props.getFilesLocation() + "/" + decodedImageDir + "/" + decodedImage), 150);
            new FileSaver(ip).saveAsJpeg(thumbnailFile.getAbsolutePath());
            log.info("[thumb:1] took:" + (System.currentTimeMillis() - start));
            //thumbnailFile.deleteOnExit();
            //fileService.deleteFile(thumbnailFile.getAbsolutePath());
            return thumbnailFile;
        }
    }
}
