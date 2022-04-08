package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.db.repository.ReportRepository;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import com.amaxilatis.metis.server.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.*;

@Slf4j
@RestController
@SuppressWarnings("SameReturnValue")
public class ReportController extends BaseController {
    
    public ReportController(final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final MetisProperties props, final BuildProperties buildProperties) {
        super(fileService, imageProcessingService, jobService, reportService, props, buildProperties);
    }
    
    @GetMapping(value = API_REPORTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTablesOutput<Report> apiReports(@Valid DataTablesInput input) {
        log.info("get:{}", API_REPORTS);
        return reportService.findAll(input);
    }
    
    @PostMapping(value = API_REPORTS)
    public DataTablesOutput<Report> getUsers(@Valid @RequestBody DataTablesInput input) {
        log.info("post:{}", API_REPORTS);
        return reportService.findAll(input);
    }
    
    @GetMapping(value = API_REPORT_DOWNLOAD, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> download(HttpServletResponse response, @PathVariable final Long reportId) throws IOException {
        log.info("get:{}, reportId:{}", API_REPORT_DOWNLOAD, reportId);
        final String fullFileName = props.getReportLocation() + "/metis-" + reportId + ".csv";
        final String xlsxName = fileService.csv2xlsx(fullFileName);
        FileUtils.sendFile(response, new File(xlsxName), xlsxName);
        return ResponseEntity.ok().build();
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @GetMapping(value = API_REPORT_DELETE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String reportDelete(@PathVariable final Long reportId) {
        log.info("get:{}, reportId:{}", API_REPORT_DELETE, reportId);
        reportService.delete(reportId);
        return "redirect:/";
    }
    
}
