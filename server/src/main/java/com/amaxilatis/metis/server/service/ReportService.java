package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.db.model.Task;
import com.amaxilatis.metis.server.db.repository.ReportRepository;
import com.amaxilatis.metis.server.db.repository.TaskRepository;
import com.amaxilatis.metis.server.model.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
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
    
    private final NotificationService notificationService;
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
    
    public void deleteTasksByReportId(long reportId, final UserDTO userDTO) {
        log.info("deleteTasksByReportId({})", reportId);
        final List<Task> tasks = taskRepository.findTasksByReportId(reportId);
        log.info("deleting {} tasks for report {}...", tasks.size(), reportId);
        int count = 0;
        for (final Task task : tasks) {
            taskRepository.deleteById(task.getId());
            count++;
        }
        if (count>0) {
            log.info("deleted {} tasks for report {}, sending notification...", count, reportId);
            notificationService.notifyCanceled(reportId, count, userDTO);
        }
    }
}
