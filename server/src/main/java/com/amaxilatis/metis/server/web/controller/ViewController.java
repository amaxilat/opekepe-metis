package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.service.BackupService;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import com.amaxilatis.metis.server.service.UserService;
import com.amaxilatis.metis.util.ImageCheckerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.SortedSet;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_HOME;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_IMAGE_DIRECTORY;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_LOG;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_LOGIN;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_SETTINGS;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_TASKS;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.VIEW_USER;

@SuppressWarnings({"SameReturnValue"})
@Slf4j
@Controller
public class ViewController extends BaseController {
    
    public ViewController(final UserService userService, final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final BackupService backupService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        super(userService, fileService, imageProcessingService, jobService, reportService, backupService, props, buildProperties, versionProperties);
    }
    
    @GetMapping(VIEW_LOGIN)
    public String login(final HttpServletRequest request, final Model model) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        prepareModel(model);
        HttpSession session = request.getSession(false);
        String loginError = null;
        if (session != null) {
            AuthenticationException ex = (AuthenticationException) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (ex != null) {
                loginError = ex.getMessage();
            }
        }
        model.addAttribute("loginError", loginError);
        return "login";
    }
    
    @GetMapping(VIEW_HOME)
    public String homePage(final Model model) {
        log.info("get:{}", VIEW_HOME);
        prepareModel(model);
        return "home";
    }
    
    @GetMapping(VIEW_SETTINGS)
    public String settingsPage(final Model model) {
        log.info("get:{}", VIEW_SETTINGS);
        prepareModel(model);
        return "settings";
    }
    
    @GetMapping(VIEW_IMAGE_DIRECTORY)
    public String homePage(final Model model, @RequestParam("dir") final String imageDirectoryHash, @RequestParam(value = "file", required = false) final String file) {
        log.info("get:{}, imageDirectoryHash:{}", VIEW_IMAGE_DIRECTORY, imageDirectoryHash);
        prepareModel(model);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        model.addAttribute("imageDir", decodedImageDir);
        model.addAttribute("imageDirectoryHash", imageDirectoryHash);
        final SortedSet<ImageFileInfo> images = fileService.listImages(decodedImageDir);
        model.addAttribute("images", images);
        int checksPerformed = images.stream().map(imageFileInfo -> fileService.getImageResults(decodedImageDir, imageFileInfo.getName()).size()).mapToInt(x -> x).sum();
        model.addAttribute("checksPerformed", checksPerformed);
        model.addAttribute("checksTotal", images.size() * 9);
        if (images.size() > 0) {
            int checkRate = (checksPerformed * 100) / (images.size() * 9);
            model.addAttribute("checkRate", checkRate);
        } else {
            model.addAttribute("checkRate", 0);
        }
        model.addAttribute("file", file);
        return "view";
    }
    
    @GetMapping(VIEW_USER)
    public String userPage(final Model model) {
        log.info("get:{}", VIEW_USER);
        prepareModel(model);
        return "user";
    }
    
    @GetMapping(VIEW_LOG)
    public String logPage(final Model model) {
        log.info("get:{}", VIEW_LOG);
        prepareModel(model);
        model.addAttribute("theLogs", ImageCheckerUtils.getActionNotes());
        return "log";
    }
    
    @GetMapping(VIEW_TASKS)
    public String tasksPage(final Model model) {
        log.info("get:{}", VIEW_TASKS);
        prepareModel(model);
        model.addAttribute("theTasks", imageProcessingService.getTasks());
        return "tasks";
    }
    
    @RequestMapping("/403")
    public String accessDenied(final Model model) {
        log.info("get:403");
        prepareModel(model);
        return "403";
    }
}
