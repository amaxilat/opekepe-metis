package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Configuration;
import com.amaxilatis.metis.server.service.BackupService;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import com.amaxilatis.metis.server.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_CLEAN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_RUN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_SAVE_CONFIGURATION;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_SETTINGS;

@SuppressWarnings({"SameReturnValue"})
@Slf4j
@Controller
public class ActionController extends BaseController {
    
    public ActionController(final UserService userService, final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final BackupService backupService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        super(userService, fileService, imageProcessingService, jobService, reportService, backupService, props, buildProperties, versionProperties);
    }
    
    @PostMapping(value = ACTION_RUN, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String run(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks, @RequestParam("notificationTargets") final String notificationTargets) {
        log.info("post:{}, name:{}, tasks:{}, notificationTargets:{}", ACTION_RUN, name, tasks, notificationTargets);
        final String decodedName = fileService.getStringFromHash(name);
        final FileJob job = FileJob.builder().name(decodedName).tasks(tasks).notificationTargets(List.of(notificationTargets.split(","))).build();
        jobService.startJob(job);
        return String.format("redirect:/view?dir=%s&successMessage=check-started", decodedName);
    }
    
    @PostMapping(value = ACTION_CLEAN, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String clean(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) {
        log.info("post:{}, name:{}, tasks:{}", ACTION_CLEAN, name, tasks);
        final String decodedName = fileService.getStringFromHash(name);
        fileService.clean(decodedName, tasks);
        return String.format("redirect:/view?dir=%s&successMessage=files-cleaned", decodedName);
    }
    
    @PostMapping(ACTION_SAVE_CONFIGURATION)
    public String postSettings(@ModelAttribute Configuration configuration) {
        log.info("post:{}", ACTION_SAVE_CONFIGURATION);
        imageProcessingService.updateConfiguration(configuration, getUsernameFromPrincipal());
        log.info(configuration.toString());
        return "redirect:" + VIEW_SETTINGS;
    }
}
