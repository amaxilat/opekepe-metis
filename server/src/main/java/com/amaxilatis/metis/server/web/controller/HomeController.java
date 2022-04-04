package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.ReportFileInfo;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.*;

@SuppressWarnings({"SameReturnValue"})
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final ReportService reportService;
    private final FileService fileService;
    private final JobService jobService;
    private final ImageProcessingService imageProcessingService;
    //private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MetisProperties props;
    
    
    @Value("${spring.application.name}")
    String appName;
    
    @GetMapping(VIEW_HOME)
    public String homePage(Model model) {
        final SortedSet<ReportFileInfo> reportSet = reportService.listReports();
        final SortedSet<ImageFileInfo> imageDirectories = fileService.getImagesDirs();
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("reports", reportSet);
        model.addAttribute("imageDirectories", imageDirectories);
        model.addAttribute("tests", imageProcessingService.getTestDescriptions());
        model.addAttribute("appName", appName);
        return "home";
    }
    
    @GetMapping(VIEW_SETTINGS)
    public String settingsPage(Model model) {
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("imageDirectories", fileService.getImagesDirs());
        model.addAttribute("metisProperties", props);
        model.addAttribute("appName", appName);
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
        model.addAttribute("appName", appName);
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
