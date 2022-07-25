package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJob;
import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import com.amaxilatis.metis.server.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_CHANGE_PASSWORD;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_CLEAN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.ACTION_RUN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_HOME;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_IMAGE_DIRECTORY;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_LOGIN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_SETTINGS;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_USERS;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_USERS_ADD;

@SuppressWarnings({"SameReturnValue"})
@Slf4j
@Controller
public class ActionController extends BaseController {
    
    public ActionController(final UserService userService, final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        super(userService, fileService, imageProcessingService, jobService, reportService, props, buildProperties, versionProperties);
    }
    
    @PostMapping(value = ACTION_RUN, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String run(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) {
        log.info("post:{}, name:{}, tasks:{}", ACTION_RUN, name, tasks);
        final String decodedName = fileService.getStringFromHash(name);
        final FileJob job = FileJob.builder().name(decodedName).tasks(tasks).build();
        jobService.startJob(job);
        return String.format("redirect:/view?dir=%s&successMessage=check-started", decodedName);
    }
    
    @PostMapping(value = ACTION_CLEAN, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String clean(@RequestParam("name") final String name, @RequestParam("tasks") final List<Integer> tasks) {
        log.info("post:{}, name:{}, tasks:{}", ACTION_CLEAN, name, tasks);
        final String decodedName = fileService.getStringFromHash(name);
        fileService.clean(decodedName, tasks);
        return String.format("redirect:/view?dir=%s&successMessage=files-cleaned", decodedName);
    }
    
    @PostMapping(value = ACTION_CHANGE_PASSWORD)
    public String apiUsers(@RequestParam("username") final String username, @RequestParam("oldPassword") final String oldPassword, @RequestParam("newPassword") final String newPassword) {
        log.info("post:{}", ACTION_CHANGE_PASSWORD);
        if (userService.updateUserPassword(username, oldPassword, newPassword)) {
            return "redirect:/?successMessage=password-changed";
        } else {
            return "redirect:/?errorMessage=password-not-changed";
        }
    }
    
    
}
