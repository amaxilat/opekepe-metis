package com.amaxilatis.metis.server.rabbit;

import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.ReportFileInfo;
import com.drew.lang.Charsets;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final MetisProperties props;
    
    public void createTempReport(final String name) {
        writeToFile(name, "", false);
    }
    
    public void append(final String name, final String text) {
        writeToFile(name, text);
    }
    
    public void append(final String name, final List<String> parts) {
        writeToFile(name, StringUtils.join(parts, ","));
    }
    
    public String csv2xlsx(final String name) {
        final String xlsxFileName = name.replace(".csv", ".xlsx");
        final File outFile = new File(xlsxFileName);
        try (final FileOutputStream fos = new FileOutputStream(outFile)) {
            final Workbook wb = new XSSFWorkbook();
            final Sheet sheet = wb.createSheet("report");
            
            final File file = new File(name);
            final FileReader fr = new FileReader(file, Charsets.UTF_8);   //reads the file
            final BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            boolean first = true;
            String line;
            while ((line = br.readLine()) != null) {
                Row row;
                if (first) {
                    row = appendRow(sheet, 0);
                    first = false;
                } else {
                    row = appendRow(sheet);
                }
                Arrays.stream(line.split(",")).forEach(s -> appendCell(row, s));
            }
            fr.close();    //closes the stream and release the resources
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
    
    public SortedSet<ReportFileInfo> listReports() {
        final List<File> reports = Arrays.stream(Objects.requireNonNull(new File(props.getReportLocation()).listFiles())).filter(file -> file.getName().endsWith(".csv")).collect(Collectors.toList());
        final SortedSet<ReportFileInfo> reportSet = new TreeSet<>();
        reports.forEach(report -> {
            try {
                final String[] parts = report.getName().replaceAll("\\.csv", "").split("-", 3);
                
                reportSet.add(ReportFileInfo.builder().directory(parts[1]).date(parts[2]).name(report.getName()).path(report.toPath().toString()).size((double) Files.size(report.toPath())).build());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });
        return reportSet;
    }
    
    public SortedSet<ImageFileInfo> listImages() {
        final SortedSet<ImageFileInfo> imagesSet = new TreeSet<>();
        final List<File> files = Arrays.stream(Objects.requireNonNull(new File(props.getFilesLocation()).listFiles())).filter(File::isDirectory).collect(Collectors.toList());
        files.forEach(file -> imagesSet.add(ImageFileInfo.builder().name(file.getName()).count(Arrays.stream(file.listFiles()).filter(file1 -> file1.getName().endsWith(".tif")).count()).build()));
        return imagesSet;
    }
}
