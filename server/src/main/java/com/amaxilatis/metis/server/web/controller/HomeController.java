package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.rabbit.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final FileService fileService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MetisProperties props;
    
    @Value("${spring.application.name}")
    String appName;
    
    @GetMapping("/")
    public String homePage(Model model) {
        final List<File> reports = Arrays.stream(Objects.requireNonNull(new File(props.getReportLocation()).listFiles())).filter(file -> file.getName().endsWith(".csv")).collect(Collectors.toList());
        final SortedMap<String, Long> sizes = new TreeMap<>();
        reports.forEach(report -> {
            try {
                sizes.put(report.getName(), Files.size(report.toPath()) / 1024);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });
        
        final List<File> files = Arrays.stream(Objects.requireNonNull(new File(props.getFilesLocation()).listFiles())).filter(File::isDirectory).collect(Collectors.toList());
        final SortedMap<String, Long> fileCounts = files.stream().collect(Collectors.toMap(File::getName, file -> Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(file1 -> file1.getName().endsWith(".tif")).count(), (a, b) -> b, TreeMap::new));
        
        model.addAttribute("reports", sizes);
        model.addAttribute("fileCounts", fileCounts);
        model.addAttribute("appName", appName);
        return "home";
    }
    
    @ResponseBody
    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] download(HttpServletResponse response, @RequestParam("name") final String name) throws IOException {
        final String fullFileName = props.getReportLocation() + "/" + name;
        final String xlsxName = fileService.csv2xlsx(fullFileName);
        final InputStream in = new FileInputStream(xlsxName);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + xlsxName + "\"");
        return IOUtils.toByteArray(in);
    }
    
    @GetMapping(value = "/run", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String run(@RequestParam("name") final String name) throws IOException {
        
        final List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        final FileJob job = FileJob.builder().name(props.getFilesLocation() + "/" + name).tasks(list).build();
        
        rabbitTemplate.convertAndSend("metis-jobs", "metis-jobs", mapper.writeValueAsString(job));
        return "redirect:/";
    }
}
