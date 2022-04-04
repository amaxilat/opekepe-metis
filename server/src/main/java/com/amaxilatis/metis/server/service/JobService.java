package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
public class JobService {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    private final MetisProperties props;
    private final ImageProcessingService imageProcessingService;
    private final FileService fileService;
    private final ReportService reportService;
    
    public void startJob(final FileJob fileJob) {
        final Date startTime = new Date(System.currentTimeMillis());
        final File targetFile = new File(props.getFilesLocation() + "/" + fileJob.getName());
        log.info("[fileJob] target:{} isDir:{} tasks:{}", fileJob.getName(), targetFile.isDirectory(), fileJob.getTasks());
        //final String outFileName = String.format("%s/metis-%s-%s.csv", props.getReportLocation(), targetFile.getName(), sdf.format(startTime));
        Report report = reportService.createReport(props, fileJob);
        fileService.createTempReport(getOutputFileName(report));
        
        final List<File> fileList = new ArrayList<>();
        if (targetFile.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(targetFile.listFiles())).filter(file -> file.getName().endsWith(".tif")).forEach(fileList::add);
        } else {
            fileList.add(targetFile);
        }
        
        fileService.append(getOutputFileName(report), "\"Φάκελος\",\"" + fileJob.getName() + "\"");
        fileService.append(getOutputFileName(report), "\"Έλεγχοι\",\"" + StringUtils.join(fileJob.getTasks(), "-") + "\"");
        // add header
        final List<String> titles = new ArrayList<>(List.of("ΑΡΧΕΙΟ"));
        fileJob.getTasks().stream().map(integer -> String.format("ΕΛΕΓΧΟΣ %d", integer)).forEach(titles::add);
        fileJob.getTasks().stream().map(integer -> String.format("ΠΑΡΑΤΗΡΗΣΕΙΣ %d", integer)).forEach(titles::add);
        fileService.append(getOutputFileName(report), titles);
        log.info("processing start {}", System.currentTimeMillis());
        fileList.forEach(file -> imageProcessingService.processFile(getOutputFileName(report), file.getPath(), fileJob.getTasks()));
    }
    
    public String getOutputFileName(final Report report) {
        return String.format("%s/metis-%d.csv", props.getReportLocation(), report.getId());
    }
}
