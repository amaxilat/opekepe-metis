package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

@Slf4j
@Getter
public class BaseController {
    
    protected final FileService fileService;
    protected final ImageProcessingService imageProcessingService;
    protected final JobService jobService;
    protected final ReportService reportService;
    protected final MetisProperties props;
    protected final BuildProperties buildProperties;
    protected final BuildVersionConfigurationProperties versionProperties;
    
    public BaseController(final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        this.fileService = fileService;
        this.imageProcessingService = imageProcessingService;
        this.jobService = jobService;
        this.reportService = reportService;
        this.props = props;
        this.buildProperties = buildProperties;
        this.versionProperties = versionProperties;
    }
    
    @Value("${spring.application.name}")
    String appName;
    
    protected void prepareMode(final Model model) {
        model.addAttribute("u", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        model.addAttribute("vp", versionProperties);
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("imageDirectories", fileService.getImagesDirs());
        model.addAttribute("metisProperties", props);
        model.addAttribute("tests", imageProcessingService.getTestDescriptions());
        model.addAttribute("bp", buildProperties);
    }
}
