package com.amaxilatis.metis.server.rabbit;

import com.amaxilatis.metis.util.Utils;
import com.amaxilatis.metis.util.model.FileJob;
import com.amaxilatis.metis.util.model.FileJobResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.exception.TikaException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class QueueReceiver {
    final ObjectMapper mapper = new ObjectMapper();
    
    @Value("${app.reportLocation}")
    String reportsLocation;
    
    @PostConstruct
    public void init() {
        final File reports = new File(reportsLocation);
        if (!reports.exists()) {
            log.info("creating reports directory...");
            reports.mkdirs();
        }
    }
    
    @RabbitListener(queuesToDeclare = @Queue(name = "metis-jobs-handler", durable = "true"), concurrency = "1-5")
    public void onMessage(Message message) {
        receive(message.getBody());
    }
    
    public void receive(final byte[] body) {
        try {
            final FileJob fileJob = mapper.readValue(new String(body), FileJob.class);
            final File targetFile = new File(fileJob.getName());
            log.info("[fileJob] target:{} isDir:{} tasks:{}", fileJob.getName(), targetFile.isDirectory(), fileJob.getTasks());
            
            final List<File> fileList = new ArrayList<>();
            if (targetFile.isDirectory()) {
                for (final File file : Objects.requireNonNull(targetFile.listFiles())) {
                    if (file.getName().endsWith(".tif")) {
                        fileList.add(file);
                    }
                }
            } else {
                fileList.add(targetFile);
            }
            
            final FileOutputStream fos = new FileOutputStream(reportsLocation + "/report-" + System.currentTimeMillis() + ".xlsx");
            final Workbook wb = new XSSFWorkbook();
            final Sheet sheet = wb.createSheet("tests");
            final Row row = appendRow(sheet, 0);
            final List<String> titles = new ArrayList<>(List.of("FILENAME"));
            fileJob.getTasks().stream().map(integer -> String.format("Test %d", integer)).forEach(titles::add);
            fileJob.getTasks().stream().map(integer -> String.format("Notes %d", integer)).forEach(titles::add);
            appendCell(row, titles);
            fileList.forEach(file -> {
                try {
                    log.info("parsing file {}", file);
                    final List<FileJobResult> results = Utils.parseFile(file, fileJob.getTasks());
                    append2Excel(sheet, results);
                    log.info("parsed file {} {}", file, results);
                } catch (IOException | TikaException | SAXException e) {
                    log.error(e.getMessage(), e);
                }
            });
            wb.write(fos);
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
    
    private void append2Excel(final Sheet sheet, final List<FileJobResult> results) {
        final Row row = appendRow(sheet);
        appendCell(row, results.get(0).getName());
        results.forEach(result -> appendCell(row, result.getResult() ? "OK" : "ERROR"));
        results.forEach(result -> {
            if (result.getResult()) {
                appendCell(row, " ");
            } else {
                appendCell(row, result.getNote());
            }
        });
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
}
