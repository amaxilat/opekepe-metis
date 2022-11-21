package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.amaxilatis.metis.server.service.FileService.CHECKS_TITLE;
import static com.amaxilatis.metis.server.service.FileService.CHECK_TITLE;
import static com.amaxilatis.metis.server.service.FileService.FILE_TITLE;
import static com.amaxilatis.metis.server.service.FileService.FOLDER_TITLE;
import static com.amaxilatis.metis.server.service.FileService.NOTES_TITLE;

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
        
        SortedSet<File> fileList = new TreeSet<>();
        if (targetFile.isDirectory()) {
            fileList = Arrays.stream(Objects.requireNonNull(targetFile.listFiles())).filter(file -> StringUtils.endsWithAny(file.getName(), ".tif", ".jp2")).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(File::getName))));
        } else {
            fileList.add(targetFile);
        }
        
        fileService.append(getOutputFileName(report), String.format("\"%s\",\"%s\"", FOLDER_TITLE, fileJob.getName()));
        fileService.append(getOutputFileName(report), String.format("\"%s\",\"%s\"", CHECKS_TITLE, StringUtils.join(fileJob.getTasks(), "-")));
        // add header
        final List<String> titles = new ArrayList<>(List.of(FILE_TITLE));
        fileJob.getTasks().stream().map(integer -> String.format(CHECK_TITLE, integer)).forEach(titles::add);
        fileJob.getTasks().stream().map(integer -> String.format(NOTES_TITLE, integer)).forEach(titles::add);
        fileService.append(getOutputFileName(report), titles);
        log.info("processing start {}", System.currentTimeMillis());
        fileList.forEach(file -> imageProcessingService.processFile(getOutputFileName(report), file.getPath(), fileJob.getTasks(), report.getId()));
    }
    
    public String getOutputFileName(final Report report) {
        return String.format("%s/metis-%d.csv", props.getReportLocation(), report.getId());
    }
}
