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
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

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
        final SortedSet<ImageFileInfo> imagedirs = fileService.listImages();
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("reports", reportSet);
        model.addAttribute("imagedirs", imagedirs);
        model.addAttribute("appName", appName);
        return "home";
    }
    
    @GetMapping(value = "/run", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String run(@RequestParam("name") final String name) throws IOException {
        final String decodedName = fileService.getStringFromHash(name);
        final List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        final FileJob job = FileJob.builder().name(props.getFilesLocation() + "/" + decodedName).tasks(list).build();
        
        rabbitTemplate.convertAndSend("metis-jobs", "metis-jobs", mapper.writeValueAsString(job));
        return "redirect:/";
    }
    
}
