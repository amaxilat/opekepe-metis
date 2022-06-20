package com.amaxilatis.metis.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class ProcessingQueueConfiguration {
    
    @Autowired
    private ProcessingProperties processingProperties;
    
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        final ThreadPoolTaskExecutor tp = new ThreadPoolTaskExecutor();
        if (processingProperties.getThreads() == -1) {
            tp.setCorePoolSize(getCpuCountBasedThreads());
            tp.setMaxPoolSize(getCpuCountBasedThreads());
        } else {
            tp.setCorePoolSize(processingProperties.getThreads());
            tp.setMaxPoolSize(processingProperties.getThreads());
        }
        tp.initialize();
        return tp;
    }
    
    private int getCpuCountBasedThreads() {
        final int cores = Runtime.getRuntime().availableProcessors();
        final int usedCores = cores == 1 ? 1 : cores - 1;
        log.info("found {} processors, defaulting to {} threads", cores, usedCores);
        return usedCores;
    }
    
}
