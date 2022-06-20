package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.UserDTO;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import com.amaxilatis.metis.server.service.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.ui.Model;

@Slf4j
@Getter
public class BaseController {
    
    protected final UserService userService;
    protected final FileService fileService;
    protected final ImageProcessingService imageProcessingService;
    protected final JobService jobService;
    protected final ReportService reportService;
    protected final MetisProperties props;
    protected final BuildProperties buildProperties;
    protected final BuildVersionConfigurationProperties versionProperties;
    
    public BaseController(final UserService userService, final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        this.userService = userService;
        this.fileService = fileService;
        this.imageProcessingService = imageProcessingService;
        this.jobService = jobService;
        this.reportService = reportService;
        this.props = props;
        this.buildProperties = buildProperties;
        this.versionProperties = versionProperties;
    }
    
    @Value("${spring.application.name}")
    String appName;
    
    protected void prepareModel(final Model model) {
        if (!(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String)) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            log.info("{}",principal);
            if (principal instanceof User){
                final User u = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                model.addAttribute("u", userService.getBySpringUser(u));
            }else if (principal instanceof LdapUserDetailsImpl){
                final LdapUserDetailsImpl u = (LdapUserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                model.addAttribute("u", UserDTO.fromUser(u));
            }
        }
        model.addAttribute("vp", versionProperties);
        model.addAttribute("pool", imageProcessingService.getPoolInfo());
        model.addAttribute("imageDirectories", fileService.getImagesDirs());
        model.addAttribute("metisProperties", props);
        model.addAttribute("tests", imageProcessingService.getTestDescriptions());
        model.addAttribute("bp", buildProperties);
    }
}
