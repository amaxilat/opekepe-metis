package com.amaxilatis.metis.server.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class HomeController {
    @Value("${spring.application.name}")
    String appName;
    
    @Value("${app.reportLocation}")
    String reportsLocation;
    
    @Value("${app.filesLocation}")
    String filesLocation;
    
    @GetMapping("/")
    public String homePage(Model model) {
        final List<File> reports = Arrays.stream(Objects.requireNonNull(new File(reportsLocation).listFiles())).filter(file -> file.getName().endsWith(".xlsx")).collect(Collectors.toList());
        final List<File> files = Arrays.stream(Objects.requireNonNull(new File(filesLocation).listFiles())).filter(File::isDirectory).collect(Collectors.toList());
        
        log.info(Arrays.toString(new File(filesLocation).listFiles()));
        model.addAttribute("reports", reports);
        model.addAttribute("filedirs", files);
        model.addAttribute("appName", appName);
        return "home";
    }
    
    @ResponseBody
    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] download(HttpServletResponse response, @RequestParam("name") final String name) throws IOException {
        final InputStream in = new FileInputStream(reportsLocation + "/" + name);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + name + "\"");
        return IOUtils.toByteArray(in);
    }
}
