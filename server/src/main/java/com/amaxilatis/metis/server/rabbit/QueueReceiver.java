package com.amaxilatis.metis.server.rabbit;

import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.util.Utils;
import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.model.FileJobResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.exception.TikaException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class QueueReceiver {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MetisProperties props;
    private final ImageProcessingService imageProcessingService;
    private final FileService fileService;
    
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    @PostConstruct
    public void init() {
        final File reports = new File(props.getReportLocation());
        if (!reports.exists()) {
            log.info("creating reports directory...");
            boolean result = reports.mkdirs();
            log.debug("created reports directory {}", result);
        }
    }
    
    
    @RabbitListener(queuesToDeclare = @Queue(name = "metis-jobs-handler", durable = "true"), concurrency = "1-5")
    public void onMessage(Message message) {
        receive(message.getBody());
    }
    
    public void receive(final byte[] body) {
        final FileJob fileJob;
        try {
            fileJob = mapper.readValue(new String(body), FileJob.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return;
        }
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
        
        final String outFileName = props.getReportLocation() + "/report-" + sdf.format(new Date(System.currentTimeMillis())) + ".csv";
        fileService.createTempReport(outFileName);
        fileService.append(outFileName, "Φάκελος: " + fileJob.getName());
        fileService.append(outFileName, "Έλεγχοι : " + StringUtils.join(fileJob.getTasks(), "-"));
        // add header
        final List<String> titles = new ArrayList<>(List.of("ΑΡΧΕΙΟ"));
        fileJob.getTasks().stream().map(integer -> String.format("ΕΛΕΓΧΟΣ %d", integer)).forEach(titles::add);
        fileJob.getTasks().stream().map(integer -> String.format("ΠΑΡΑΤΗΡΗΣΕΙΣ %d", integer)).forEach(titles::add);
        fileService.append(outFileName, titles);
        fileList.forEach(file -> imageProcessingService.processFile(outFileName, file.getPath(), fileJob.getTasks()));
    }
}
