package com.amaxilatis.metis.server.rabbit;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.service.JobService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;


@Slf4j
@Service
@RequiredArgsConstructor
public class QueueReceiver {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MetisProperties props;
    private final JobService jobService;
    
    @PostConstruct
    public void init() {
        final File reports = new File(props.getReportLocation());
        if (!reports.exists()) {
            log.info("creating reports directory...");
            boolean result = reports.mkdirs();
            log.debug("created reports directory {}", result);
        }
    }
    
    public void receive(final byte[] body) {
        final FileJob fileJob;
        try {
            fileJob = mapper.readValue(new String(body), FileJob.class);
            jobService.startJob(fileJob);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return;
        }
    }
}
