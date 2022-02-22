package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.config.ProcessingProperties;
import com.amaxilatis.metis.server.model.TestDescription;
import com.amaxilatis.metis.server.rabbit.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {
    
    private final FileService fileService;
    private final SimpMessagingTemplate webSocketService;
    private final ProcessingProperties processingProperties;
    
    private ThreadPoolTaskExecutor taskExecutor;
    
    private final SortedSet<TestDescription> testDescriptions = new TreeSet<>();
    
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
        
        testDescriptions.add(TestDescription.builder().id(1).name("Έλεγχος 1: Χωρική Ανάλυση").enabled(true).build());
        testDescriptions.add(TestDescription.builder().id(2).name("Έλεγχος 2: Ραδιομετρική Ανάλυση").enabled(true).build());
        testDescriptions.add(TestDescription.builder().id(3).name("Έλεγχος 3: Φασματική Ανάλυση").enabled(true).build());
        testDescriptions.add(TestDescription.builder().id(4).name("Έλεγχος 4: Νεφοκάλυψη").enabled(false).build());
        testDescriptions.add(TestDescription.builder().id(5).name("Έλεγχος 5: Ολικό clipping").enabled(true).build());
        testDescriptions.add(TestDescription.builder().id(6).name("Έλεγχος 6: Κορυφής Ιστογράμματος").enabled(true).build());
        testDescriptions.add(TestDescription.builder().id(7).name("Έλεγχος 7: Αντίθεσης").enabled(false).build());
        testDescriptions.add(TestDescription.builder().id(8).name("Έλεγχος 8: Συμπίεσης").enabled(false).build());
        testDescriptions.add(TestDescription.builder().id(9).name("Έλεγχος 9: Ομοιογενών Αντικειμένων").enabled(false).build());
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
    
    public void processFile(final String outFileName, final String filename, final List<Integer> tasks) {
        taskExecutor.execute(new ImageProcessingTask(fileService, outFileName, filename, tasks));
    }
    
    public PoolInfo getPoolInfo() {
        final long pending = taskExecutor.getThreadPoolExecutor().getTaskCount() - taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
        return new PoolInfo(taskExecutor.getCorePoolSize(), taskExecutor.getPoolSize(), taskExecutor.getActiveCount(), pending);
    }
    
    public Set<TestDescription> getTestDescriptions() {
        return testDescriptions;
    }
}
