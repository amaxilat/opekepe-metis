package com.amaxilatis.metis.server.rabbit;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    @Bean
    TopicExchange exchange() {
        return new TopicExchange("metis-jobs", true, false);
    }
    
    @Bean
    Queue queue() {
        return new Queue("metis-jobs-handler", true);
    }
    
    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("#");
    }
    
    @PostConstruct
    public void init() {
        final File reports = new File(props.getReportLocation());
        if (!reports.exists()) {
            log.info("creating reports directory...");
            boolean result = reports.mkdirs();
            log.debug("created reports directory {}", result);
        }
    }
    
    @RabbitListener(queues = "metis-jobs-handler", concurrency = "1-5")
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
        
        final Date startTime = new Date(System.currentTimeMillis());
        final File targetFile = new File(fileJob.getName());
        log.info("[fileJob] target:{} isDir:{} tasks:{}", fileJob.getName(), targetFile.isDirectory(), fileJob.getTasks());
        final String outFileName = String.format("%s/metis-%s-%s.csv", props.getReportLocation(), targetFile.getName(), sdf.format(startTime));
        fileService.createTempReport(outFileName);
        
        final List<File> fileList = new ArrayList<>();
        if (targetFile.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(targetFile.listFiles())).filter(file -> file.getName().endsWith(".tif")).forEach(fileList::add);
        } else {
            fileList.add(targetFile);
        }
        
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
