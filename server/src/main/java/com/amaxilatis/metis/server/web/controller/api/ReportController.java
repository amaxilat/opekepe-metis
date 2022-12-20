package com.amaxilatis.metis.server.web.controller.api;

import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import com.amaxilatis.metis.server.service.UserService;
import com.amaxilatis.metis.server.util.FileUtils;
import com.amaxilatis.metis.server.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.File;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_DIRECTORY_REPORT_DOWNLOAD;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_REPORTS;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_REPORT_DELETE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_REPORT_DOWNLOAD;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.IMAGE_DIR_HASH;

@Slf4j
@RestController
@SuppressWarnings("SameReturnValue")
public class ReportController extends BaseController {
    
    public ReportController(final UserService userService, final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        super(userService, fileService, imageProcessingService, jobService, reportService, props, buildProperties, versionProperties);
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
    public ResponseEntity<Resource> download(@PathVariable final Long reportId) {
        log.info("get:{}, reportId:{}", API_REPORT_DOWNLOAD, reportId);
        final String fullFileName = props.getReportLocation() + "/metis-" + reportId + ".csv";
        final String xlsxName = fileService.csv2xlsx(fullFileName);
        return FileUtils.sendFile(new File(xlsxName), xlsxName);
    }
    
    @GetMapping(value = API_REPORT_DELETE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String reportDelete(@PathVariable final Long reportId) {
        log.info("get:{}, reportId:{}", API_REPORT_DELETE, reportId);
        reportService.delete(reportId);
        return "redirect:/";
    }
    
    @GetMapping(value = API_DIRECTORY_REPORT_DOWNLOAD, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadDirectoryReport(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash) {
        log.info("get:{}, imageDirectoryHash:{}", API_DIRECTORY_REPORT_DOWNLOAD, imageDirectoryHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final File xlsxFile = fileService.generateDirectoryReportXlsx(decodedImageDir);
        return FileUtils.sendFile(xlsxFile, xlsxFile.getName());
    }
    
}
