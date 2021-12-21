package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.rabbit.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ReportController {
    
    private final FileService fileService;
    private final MetisProperties props;
    
    
    @ResponseBody
    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] download(HttpServletResponse response, @RequestParam("name") final String name) throws IOException {
        final String fullFileName = props.getReportLocation() + "/" + name;
        final String xlsxName = fileService.csv2xlsx(fullFileName);
        final InputStream in = new FileInputStream(xlsxName);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + xlsxName + "\"");
        return IOUtils.toByteArray(in);
    }
    
    @GetMapping(value = "/report/delete", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String reportDelete(@RequestParam("hash") final String hash) throws IOException {
        final String decodedName = fileService.getStringFromHash(hash);
        final String decodedNameXlsx = fileService.csv2xlsxName(decodedName);
        new File(props.getReportLocation() + "/" + decodedName).delete();
        new File(props.getReportLocation() + "/" + decodedNameXlsx).delete();
        return "redirect:/";
    }
    
}
