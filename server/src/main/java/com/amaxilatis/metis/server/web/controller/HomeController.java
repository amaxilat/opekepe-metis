package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_CLEAN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_RUN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_HOME;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_IMAGE_DIRECTORY;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_SETTINGS;

@SuppressWarnings({"SameReturnValue"})
@Slf4j
@Controller
public class HomeController extends BaseController {
    
    public HomeController(final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        super(fileService, imageProcessingService, jobService, reportService, props, buildProperties, versionProperties);
    }
    
    @GetMapping(VIEW_HOME)
    public String homePage(final Model model) {
        log.info("get:{}", VIEW_HOME);
        prepareMode(model);
        return "home";
    }
    
    @GetMapping(VIEW_SETTINGS)
    public String settingsPage(final Model model) {
        log.info("get:{}", VIEW_SETTINGS);
        prepareMode(model);
        return "settings";
    }
    
    @GetMapping(VIEW_IMAGE_DIRECTORY)
    public String homePage(final Model model, @PathVariable("imageDirectoryHash") final String imageDirectoryHash) {
        log.info("get:{}, imageDirectoryHash:{}", VIEW_IMAGE_DIRECTORY, imageDirectoryHash);
        prepareMode(model);
        final String decodedImageDir = getFileService().getStringFromHash(imageDirectoryHash);
        model.addAttribute("imageDir", decodedImageDir);
        model.addAttribute("imageDirectoryHash", imageDirectoryHash);
        model.addAttribute("images", getFileService().listImages(decodedImageDir));
        return "view";
    }
    
    @PostMapping(value = ACTION_RUN, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String run(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) {
        log.info("post:{}, name:{}, tasks:{}", ACTION_RUN, name, tasks);
        final String decodedName = getFileService().getStringFromHash(name);
        final FileJob job = FileJob.builder().name(decodedName).tasks(tasks).build();
        getJobService().startJob(job);
        return "redirect:/";
    }
    
    @PostMapping(value = ACTION_CLEAN, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String clean(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) {
        log.info("post:{}, name:{}, tasks:{}", ACTION_CLEAN, name, tasks);
        final String decodedName = getFileService().getStringFromHash(name);
        getFileService().clean(decodedName, tasks);
        return "redirect:/";
    }
    
}
