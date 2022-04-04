package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ReportFileInfo;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.db.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final MetisProperties props;
    
    public Report createReport(final MetisProperties props, final FileJob fileJob) {
        Report report = new Report();
        report.setFilesLocation(props.getFilesLocation());
        report.setReportLocation(props.getReportLocation());
        report.setPath(fileJob.getName());
        report.setDate(new Date());
        return reportRepository.save(report);
        //report.setOutputFileName(String.format("%s/metis-%d.csv", props.getReportLocation(), report.getId()));
        //return reportRepository.save(report);
    }
    
    public SortedSet<ReportFileInfo> listReports() {
        final SortedSet<ReportFileInfo> reports = new TreeSet<>();
        for (final Report report : reportRepository.findAll()) {
            log.info("report: {}", report);
            reports.add(ReportFileInfo.builder()
                    //
                    .id(report.getId())
                    //
                    .directory(report.getPath())
                    //.directory(report.getOutputFileName())
                    .date(report.getDate()).build());
        }
        return reports;
    }
    
    public void delete(Long reportId) {
        reportRepository.deleteById(reportId);
        new File(props.getReportLocation() + "/metis-" + reportId + ".csv").delete();
        new File(props.getReportLocation() + "/metis-" + reportId + ".xlsx").delete();
    }
}
