package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.ReportFileInfo;
import com.amaxilatis.metis.server.rabbit.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

@SuppressWarnings({"SameReturnValue"})
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final FileService fileService;
    private final ImageProcessingService imageProcessingService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MetisProperties props;
    
    
    @Value("${spring.application.name}")
    String appName;
    
    @GetMapping("/")
    public String homePage(Model model) {
        final SortedSet<ReportFileInfo> reportSet = fileService.listReports();
        final SortedSet<ImageFileInfo> imageDirectories = fileService.listImages();
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("reports", reportSet);
        model.addAttribute("imageDirectories", imageDirectories);
        model.addAttribute("tests", imageProcessingService.getTestDescriptions());
        model.addAttribute("appName", appName);
        return "home";
    }
    
    @GetMapping("/view/{imageDirectoryHash}")
    public String homePage(Model model, @PathVariable("imageDirectoryHash") String imageDirectoryHash) {
        log.info("dirHash = {}", imageDirectoryHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final SortedSet<ImageFileInfo> images = fileService.listImages(decodedImageDir);
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("images", images);
        model.addAttribute("imageDir", decodedImageDir);
        model.addAttribute("imageDirectoryHash", imageDirectoryHash);
        model.addAttribute("tests", imageProcessingService.getTestDescriptions());
        model.addAttribute("appName", appName);
        return "view";
    }
    
    @PostMapping(value = "/run", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String run(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) throws IOException {
        
        final String decodedName = fileService.getStringFromHash(name);
        log.info("run tasks: {}, decodedName: {}", tasks, decodedName);
        final FileJob job = FileJob.builder().name(props.getFilesLocation() + "/" + decodedName).tasks(tasks).build();
        
        rabbitTemplate.convertAndSend("metis-jobs", "metis-jobs", mapper.writeValueAsString(job));
        return "redirect:/";
    }
    
    @PostMapping(value = "/clean", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String clean(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) {
        final String decodedName = fileService.getStringFromHash(name);
        fileService.clean(decodedName, tasks);
        return "redirect:/";
    }
    
}
