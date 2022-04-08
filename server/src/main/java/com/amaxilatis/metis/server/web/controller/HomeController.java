package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_CLEAN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_RUN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_HOME;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_IMAGE_DIRECTORY;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_SETTINGS;

@SuppressWarnings({"SameReturnValue"})
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final FileService fileService;
    private final JobService jobService;
    private final ImageProcessingService imageProcessingService;
    private final MetisProperties props;
    @Autowired
    BuildProperties buildProperties;
    
    @Value("${spring.application.name}")
    String appName;
    
    @GetMapping(VIEW_HOME)
    public String homePage(Model model) {
        final SortedSet<ImageFileInfo> imageDirectories = fileService.getImagesDirs();
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("imageDirectories", imageDirectories);
        model.addAttribute("tests", imageProcessingService.getTestDescriptions());
        model.addAttribute("bp", buildProperties);
        return "home";
    }
    
    @GetMapping(VIEW_SETTINGS)
    public String settingsPage(Model model) {
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("imageDirectories", fileService.getImagesDirs());
        model.addAttribute("metisProperties", props);
        model.addAttribute("bp", buildProperties);
        return "settings";
    }
    
    @GetMapping(VIEW_IMAGE_DIRECTORY)
    public String homePage(Model model, @PathVariable("imageDirectoryHash") String imageDirectoryHash) {
        log.info("dirHash = {}", imageDirectoryHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final SortedSet<ImageFileInfo> imageDirectories = fileService.getImagesDirs();
        final SortedSet<ImageFileInfo> images = fileService.listImages(decodedImageDir);
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("imageDirectories", imageDirectories);
        model.addAttribute("images", images);
        model.addAttribute("imageDir", decodedImageDir);
        model.addAttribute("imageDirectoryHash", imageDirectoryHash);
        model.addAttribute("tests", imageProcessingService.getTestDescriptions());
        model.addAttribute("bp", buildProperties);
        return "view";
    }
    
    @PostMapping(value = ACTION_RUN, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String run(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) throws IOException {
        
        final String decodedName = fileService.getStringFromHash(name);
        log.info("run tasks: {}, decodedName: {}", tasks, decodedName);
        final FileJob job = FileJob.builder().name(decodedName).tasks(tasks).build();
        jobService.startJob(job);
        return "redirect:/";
    }
    
    @PostMapping(value = ACTION_CLEAN, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String clean(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) {
        final String decodedName = fileService.getStringFromHash(name);
        fileService.clean(decodedName, tasks);
        return "redirect:/";
    }
    
}
