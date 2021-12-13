package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.rabbit.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {
    
    private final FileService fileService;
    private final SimpMessagingTemplate webSocketService;
    
    private ThreadPoolTaskExecutor taskExecutor;
    
    @PostConstruct
    public void init() {
        final ThreadPoolTaskExecutor tp = new ThreadPoolTaskExecutor();
        tp.setCorePoolSize(6);
        tp.initialize();
        this.taskExecutor = tp;
    }
    
    @Scheduled(fixedRate = 10000L)
    public void logPool() {
        final long pending = taskExecutor.getThreadPoolExecutor().getTaskCount() - taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
        log.info("[pool] size:{} active:{} pending:{}", taskExecutor.getPoolSize(), taskExecutor.getActiveCount(), pending);
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
