package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.db.repository.ReportRepository;
import com.amaxilatis.metis.server.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@SuppressWarnings("SameReturnValue")
public class ReportController {
    
    private final FileService fileService;
    private final ReportService reportService;
    private final ReportRepository reportRepository;
    private final MetisProperties props;
    
    @GetMapping(value = API_REPORTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTablesOutput<Report> apiReports(@Valid DataTablesInput input) {
        return reportRepository.findAll(input);
        //        return reportService.listReports();
    }
    
    @PostMapping(value = API_REPORTS)
    public DataTablesOutput<Report> getUsers(@Valid @RequestBody DataTablesInput input) {
        return reportRepository.findAll(input);
    }
    
    
    @GetMapping(value = API_REPORT_DOWNLOAD, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] download(HttpServletResponse response, @PathVariable final Long reportId) throws IOException {
        final String fullFileName = props.getReportLocation() + "/metis-" + reportId + ".csv";
        final String xlsxName = fileService.csv2xlsx(fullFileName);
        final InputStream in = new FileInputStream(xlsxName);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + xlsxName + "\"");
        return IOUtils.toByteArray(in);
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @GetMapping(value = API_REPORT_DELETE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String reportDelete(@PathVariable final Long reportId) {
        reportService.delete(reportId);
        return "redirect:/";
    }
    
}
