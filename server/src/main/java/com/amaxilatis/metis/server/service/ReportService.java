package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.db.model.Task;
import com.amaxilatis.metis.server.db.repository.ReportRepository;
import com.amaxilatis.metis.server.db.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final MetisProperties props;
    
    public Report createReport(final MetisProperties props, final FileJob fileJob) {
        Report report = new Report();
        report.setFilesLocation(props.getFilesLocation());
        report.setReportLocation(props.getReportLocation());
        report.setNotificationTargets(StringUtils.join(fileJob.getNotificationTargets(), ','));
        report.setPath(fileJob.getName());
        report.setDate(new Date());
        return reportRepository.save(report);
    }
    
    public void delete(final Long reportId) {
        reportRepository.deleteById(reportId);
        new File(props.getReportLocation() + "/metis-" + reportId + ".csv").delete();
        new File(props.getReportLocation() + "/metis-" + reportId + ".xlsx").delete();
    }
    
    public DataTablesOutput<Report> findAll(final DataTablesInput input) {
        return reportRepository.findAll(input);
    }
    
    @Scheduled(cron = "0 0 0 * * ?")
    public void backup() {
        if (StringUtils.isEmpty(props.getDbBackupLocation())) {
            return;
        }
        final String backupLocation = props.getDbBackupLocation() + String.format("\\%d-metisdb-backup\\", System.currentTimeMillis());
        try {
            log.info("backing up database to {}", backupLocation);
            reportRepository.backup(backupLocation);
        } catch (Exception e) {
            log.info("stored database backup in {}", backupLocation);
        }
    }
    
    public void deleteTasksByReportId(long reportId) {
        log.info("deleteTasksByReportId({})", reportId);
        final List<Task> tasks = taskRepository.findTasksByReportId(reportId);
        log.info("deleting {} tasks for report {}...", tasks.size(), reportId);
        int count = 0;
        for (final Task task : tasks) {
            taskRepository.deleteById(task.getId());
            count++;
        }
        log.info("deleted {} tasks for report {}...", count, reportId);
    }
}
