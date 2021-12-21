package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.PoolInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ApiController {
    
    private final ImageProcessingService imageProcessingService;
    
    @ResponseBody
    @GetMapping(value = "/api/pool", produces = MediaType.APPLICATION_JSON_VALUE)
    public PoolInfo apiPool() {
        return imageProcessingService.getPoolInfo();
    }
    
}
