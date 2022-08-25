package com.amaxilatis.metis.server.web.controller.api;

import com.amaxilatis.metis.detector.client.dto.PingDataDTO;
import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.PoolInfo;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_BACKUP;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_CLOUD;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_CLOUDCOVER_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_HISTOGRAM_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_IMAGE_DIRECTORY;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_IMAGE_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_POOL;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_SCAN_IMAGES;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_THUMBNAIL_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.IMAGE_DIR_HASH;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.IMAGE_HASH;

@Slf4j
@Controller
public class ApiController extends BaseController {
    
    public ApiController(final UserService userService, final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        super(userService, fileService, imageProcessingService, jobService, reportService, props, buildProperties, versionProperties);
    }
    
    @ResponseBody
    @GetMapping(value = API_POOL, produces = MediaType.APPLICATION_JSON_VALUE)
    public PoolInfo apiPool() {
        log.info("get:{}", API_POOL);
        return imageProcessingService.getPoolInfo();
    }
    
    @ResponseBody
    @GetMapping(value = API_CLOUD, produces = MediaType.APPLICATION_JSON_VALUE)
    public PingDataDTO apiCloud() {
        log.info("get:{}", API_CLOUD);
        return imageProcessingService.getCloudInfo();
    }
    
    @ResponseBody
    @GetMapping(value = API_SCAN_IMAGES, produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> scanImages() {
        log.info("get:{}", API_SCAN_IMAGES);
        fileService.updateImageDirs(true);
        return apiImages();
    }
    
    @ResponseBody
    @GetMapping(value = API_IMAGE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> apiImages() {
        log.info("get:{}", API_IMAGE);
        return fileService.getImagesDirs();
    }
    
    @ResponseBody
    @GetMapping(value = API_IMAGE_DIRECTORY, produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> apiImagesInDirectory(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash) {
        log.info("get:{}, imageDirectoryHash:{}", API_IMAGE_DIRECTORY, imageDirectoryHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        return fileService.listImages(decodedImageDir);
    }
    
    @ResponseBody
    @GetMapping(value = API_IMAGE_DIRECTORY_IMAGE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileJobResult> apiImages(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_IMAGE_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        return fileService.getImageResults(decodedImageDir, decodedImage);
    }
    
    @GetMapping(value = API_THUMBNAIL_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageThumb(final HttpServletResponse response, @PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) throws IOException {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_THUMBNAIL_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File thumbnailFile = fileService.getImageThumbnail(decodedImageDir, decodedImage);
        return thumbnailFile != null ? FileUtils.sendFile(response, thumbnailFile, thumbnailFile.getName()) : null;
    }
    
    @GetMapping(value = API_HISTOGRAM_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageHistogram(final HttpServletResponse response, @PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) throws IOException {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_HISTOGRAM_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File histogramFile = fileService.getImageHistogram(decodedImageDir, decodedImage);
        return histogramFile != null ? FileUtils.sendFile(response, histogramFile, histogramFile.getName()) : null;
    }
    
    @GetMapping(value = API_CLOUDCOVER_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageCloudCover(final HttpServletResponse response, @PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) throws IOException {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_CLOUDCOVER_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File cloudCoverFile = fileService.getImageCloudCover(decodedImageDir, decodedImage);
        return cloudCoverFile != null ? FileUtils.sendFile(response, cloudCoverFile, cloudCoverFile.getName()) : null;
    }
    
    @ResponseBody
    @GetMapping(value = API_BACKUP)
    public String apiBackup() {
        reportService.backup();
        return "ok";
    }
    
}
