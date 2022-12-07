package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.db.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_IMAGE_DIRECTORY;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final JavaMailSender emailSender;
    private final ReportRepository reportRepository;
    private final FileService fileService;
    private final MetisProperties props;
    
    public void notify(final Long reportId) {
        log.info("[report:{}] notify", reportId);
        final Optional<Report> optReport = reportRepository.findById(reportId);
        if (optReport.isPresent()) {
            final Report report = optReport.get();
            final String targetsList = report.getNotificationTargets();
            if (StringUtils.isNotEmpty(targetsList)) {
                Arrays.stream(targetsList.split(",")).forEach(target -> sendMailWithAttachment(
                        //report id
                        report.getId(),
                        //destination
                        target,
                        //subject
                        String.format("Ειδοποίηση ολοκλήρωσης Ελέγχων: %s", report.getPath()),
                        //body
                        String.format("Οι έλεγχοι στο φάκελο %s έχουν ολοκληρωθεί. Δείτε περισσότερα εδώ: %s%s?dir=%s", report.getPath(), props.getDomain(), VIEW_IMAGE_DIRECTORY, report.getPath())));
            } else {
                log.info("[report:{}] no notification targets", reportId);
            }
        }
    }
    
    public void sendMailWithAttachment(final long reportId, final String to, final String subject, final String body) {
        log.info("[report:{}] should notify target: {}", reportId, to);
        final String xlsxName = fileService.csv2xlsx(String.format("%s/metis-%d.csv", props.getReportLocation(), reportId));
        sendMailWithAttachment(to, subject, body, new File(xlsxName));
    }
    
    public void sendMailWithAttachment(final String to, final String subject, final String body, final File attachment) {
        try {
            emailSender.send(mimeMessage -> {
                final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                message.setTo(new InternetAddress(to));
                message.setFrom(new InternetAddress(props.getNotificationEmail()));
                message.setSubject(subject);
                message.setText(body, true);
                message.addAttachment(attachment.getName(), new ByteArrayResource(IOUtils.toByteArray(new FileInputStream(attachment))), Files.probeContentType(attachment.toPath()));
                mimeMessage.setContent(body, "text/html; charset=utf-8");
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
