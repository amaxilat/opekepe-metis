package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.db.model.Report;
import com.amaxilatis.metis.server.db.repository.ReportRepository;
import com.amaxilatis.metis.server.model.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.File;
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
    
    public void notifyCanceled(long reportId, int count, UserDTO userDTO) {
        log.info("[report:{}] notifyCanceled", reportId);
        final Optional<Report> optReport = reportRepository.findById(reportId);
        if (optReport.isPresent()) {
            final Report report = optReport.get();
            final String targetsList = report.getNotificationTargets();
            if (StringUtils.isNotEmpty(targetsList)) {
                String bodyText = String.format("Οι %d υπολειπόμενοι έλεγχοι στο φάκελο %s έχουν ακυρωθεί ", count, report.getPath());
                if (userDTO != null) {
                    bodyText += String.format("από το χρήστη %s", userDTO.getUsername());
                }
                final String finalBodyText = bodyText;
                Arrays.stream(targetsList.split(",")).forEach(target -> sendMail(
                        //report id
                        report.getId(),
                        //destination
                        target,
                        //subject
                        String.format("Ειδοποίηση Ακύρωσης Ελέγχων: %s", report.getPath()),
                        //body
                        finalBodyText));
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
    
    public void sendMail(final long reportId, final String to, final String subject, final String body) {
        log.info("[report:{}] should notify target: {}", reportId, to);
        sendMailWithAttachment(to, subject, body, null);
    }
    
    public void sendMailWithAttachment(final String to, final String subject, final String body, final File attachment) {
        try {
            emailSender.send(mimeMessage -> {
                mimeMessage.setFrom(props.getNotificationEmail());
                mimeMessage.setSubject(subject);
                mimeMessage.addRecipients(Message.RecipientType.TO, to);
                
                final Multipart multipart = new MimeMultipart();
                
                final BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(body);
                multipart.addBodyPart(messageBodyPart);
    
                if (attachment != null) {
                    final MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(attachment);
                    multipart.addBodyPart(attachmentPart);
                }
                
                mimeMessage.setContent(multipart);
            });
            log.info("sent mail to: {}", to);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
