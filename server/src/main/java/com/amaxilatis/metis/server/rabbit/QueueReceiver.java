package com.amaxilatis.metis.server.rabbit;

import com.amaxilatis.metis.server.model.FileJob;
import com.amaxilatis.metis.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class QueueReceiver {
    final ObjectMapper mapper = new ObjectMapper();
    
    @RabbitListener(queues = "spring-boot", concurrency = "1-5")
    public void onMessage(Message message) {
        receive(message.getBody());
    }
    
    public void receive(final byte[] body) {
        try {
            final FileJob fileJob = mapper.readValue(new String(body), FileJob.class);
            File targetFile = new File(fileJob.getName());
            log.info("[fileJob] target:{} isDir:{} tasks:{}", fileJob.getName(), targetFile.isDirectory(), fileJob.getTasks());
            
            List<File> fileList = new ArrayList<File>();
            if (targetFile.isDirectory()) {
                for (File file : targetFile.listFiles()) {
                    if (file.getName().endsWith(".tif")) {
                        fileList.add(file);
                    }
                }
            } else {
                fileList.add(targetFile);
            }
            if (fileJob.getTasks().contains(0)) {
                fileList.forEach(file -> {
                    try {
                        Utils.parseFile(file);
                    } catch (IOException | TikaException | SAXException e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
            if (fileJob.getTasks().contains(1)) {
                fileList.forEach(file -> {
                    try {
                        Utils.testN1(file);
                    } catch (IOException | TikaException | SAXException e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
            if (fileJob.getTasks().contains(2)) {
                fileList.forEach(file -> {
                    try {
                        Utils.testN2(file);
                    } catch (IOException | TikaException | SAXException e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
            if (fileJob.getTasks().contains(3)) {
                fileList.forEach(file -> {
                    try {
                        Utils.testN3(file);
                    } catch (IOException | TikaException | SAXException e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
    
}
