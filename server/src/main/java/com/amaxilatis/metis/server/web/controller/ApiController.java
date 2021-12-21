package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.ReportFileInfo;
import com.amaxilatis.metis.server.rabbit.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.PoolInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.SortedSet;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ApiController {
    
    private final ImageProcessingService imageProcessingService;
    private final FileService fileService;
    
    @ResponseBody
    @GetMapping(value = "/api/pool", produces = MediaType.APPLICATION_JSON_VALUE)
    public PoolInfo apiPool() {
        return imageProcessingService.getPoolInfo();
    }
    
    @ResponseBody
    @GetMapping(value = "/api/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ReportFileInfo> apiReports() {
        return fileService.listReports();
    }
    
    @ResponseBody
    @GetMapping(value = "/api/image", produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> apiImages() {
        return fileService.listImages();
    }
    
}
