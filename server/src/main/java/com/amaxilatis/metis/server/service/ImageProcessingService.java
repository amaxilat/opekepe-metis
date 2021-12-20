package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.config.ProcessingProperties;
import com.amaxilatis.metis.server.rabbit.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {
    
    private final FileService fileService;
    private final SimpMessagingTemplate webSocketService;
    private final ProcessingProperties processingProperties;
    
    private ThreadPoolTaskExecutor taskExecutor;
    
    @PostConstruct
    public void init() {
        final ThreadPoolTaskExecutor tp = new ThreadPoolTaskExecutor();
        if (processingProperties.getThreads() == -1) {
            tp.setCorePoolSize(getCpuCountBasedThreads());
        } else {
            tp.setCorePoolSize(processingProperties.getThreads());
        }
        tp.initialize();
        this.taskExecutor = tp;
    }
    
    private int getCpuCountBasedThreads() {
        final int cores = Runtime.getRuntime().availableProcessors();
        final int usedCores = cores == 1 ? 1 : cores - 1;
        log.info("found {} processors, defaulting to {} threads", cores, usedCores);
        return usedCores;
    }
    
    @Scheduled(fixedRate = 10000L)
    public void logPool() {
        final long pending = taskExecutor.getThreadPoolExecutor().getTaskCount() - taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
        log.debug("[pool] size:{} active:{} pending:{}", taskExecutor.getPoolSize(), taskExecutor.getActiveCount(), pending);
    }
    
    @Scheduled(fixedRate = 1000L)
    public void updatePool() {
        webSocketService.convertAndSend("/topic/pool", getPoolInfo());
    }
    
    public void processFile(final String outFileName, final String name, final List<Integer> tasks) {
        taskExecutor.execute(new ImageProcessingTask(fileService, outFileName, name, tasks));
    }
    
    public PoolInfo getPoolInfo() {
        final long pending = taskExecutor.getThreadPoolExecutor().getTaskCount() - taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
        return new PoolInfo(taskExecutor.getCorePoolSize(), taskExecutor.getPoolSize(), taskExecutor.getActiveCount(), pending);
    }
}
