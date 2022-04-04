package com.amaxilatis.metis.server.service;

import com.adobe.internal.xmp.impl.Base64;
import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.ReportFileInfo;
import com.drew.lang.Charsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.util.stream.Collectors;

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
    
//    public SortedSet<ReportFileInfo> listReports() {
//        final List<File> reports = Arrays.stream(Objects.requireNonNull(new File(props.getReportLocation()).listFiles())).filter(file -> file.getName().endsWith(".csv")).collect(Collectors.toList());
//        final SortedSet<ReportFileInfo> reportSet = new TreeSet<>();
//        reports.forEach(report -> {
//            try {
//                final String[] parts = report.getName().replaceAll("\\.csv", "").split("-", 3);
//
//                //                reportSet.add(ReportFileInfo.builder().directory(parts[1]).date(parts[2]).name(report.getName()).hash(getStringHash(report.getName())).path(report.toPath().toString()).size((double) Files.size(report.toPath())).build());
//                reportSet.add(ReportFileInfo.builder()
//                        .directory(parts[1])
//                        .name(report.getName())
//                        .hash(getStringHash(report.getName()))
//                        .path(report.toPath().toString())
//                        .size((double) Files.size(report.toPath()))
//                        .build());
//            } catch (IOException e) {
//                log.error(e.getMessage(), e);
//            }
//        });
//        return reportSet;
//    }
    
    
    Map<String, SortedSet<ImageFileInfo>> images = new HashMap<>();
    SortedSet<ImageFileInfo> imagesDirs = new TreeSet<>();
    
    @PostConstruct
    public void init() {
        updateImageDirs();
    }
    
    public void updateImageDirs() {
        long start = System.currentTimeMillis();
        imagesDirs.clear();
        images.clear();
        final File[] imageDirectoryList = new File(props.getFilesLocation()).listFiles(File::isDirectory);
        if (imageDirectoryList != null) {
            for (final File imagesDirectory : imageDirectoryList) {
                final String imagesDirectoryName = imagesDirectory.getName();
                images.put(imagesDirectoryName, new TreeSet<>());
                final Set<ImageFileInfo> imageSet = new HashSet<>();
                long count = 0;
                final File[] filesList = imagesDirectory.listFiles(File::isFile);
                if (filesList != null) {
                    for (final File image : filesList) {
                        if (image.getName().endsWith(".tif")) {
                            count++;
                            imageSet.add(ImageFileInfo.builder().name(image.getName()).hash(getStringHash(image.getName())).build());
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
}
