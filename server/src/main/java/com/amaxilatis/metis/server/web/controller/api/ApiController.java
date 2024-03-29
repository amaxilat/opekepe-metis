package com.amaxilatis.metis.server.web.controller.api;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.config.BuildVersionConfigurationProperties;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.PoolInfo;
import com.amaxilatis.metis.server.service.BackupService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.List;
import java.util.SortedSet;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_BACKUP;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_BSI_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_CLOUD_COVER_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_COLOR_BALANCE_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_HISTOGRAM_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_IMAGE_DIRECTORY;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_IMAGE_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_NDWI_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_NIR_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_POOL;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_SCAN_IMAGES;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_THUMBNAIL_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.API_WATER_DIRECTORY_IMAGE;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.IMAGE_DIR_HASH;
import static com.amaxilatis.metis.server.web.controller.ApiRoutes.IMAGE_HASH;

@Slf4j
@Controller
public class ApiController extends BaseController {
    
    public ApiController(final UserService userService, final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final BackupService backupService, final MetisProperties props, final BuildProperties buildProperties, final BuildVersionConfigurationProperties versionProperties) {
        super(userService, fileService, imageProcessingService, jobService, reportService, backupService, props, buildProperties, versionProperties);
    }
    
    @ResponseBody
    @GetMapping(value = API_POOL, produces = MediaType.APPLICATION_JSON_VALUE)
    public PoolInfo apiPool() {
        log.info("get:{}", API_POOL);
        return imageProcessingService.getPoolInfo();
    }
    
    @ResponseBody
    @GetMapping(value = API_SCAN_IMAGES, produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> scanImages(@RequestParam("cleanup") boolean cleanup) {
        log.info("get:{} cleanup: {}", API_SCAN_IMAGES, cleanup);
        fileService.updateImageDirs(true, cleanup);
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
    public ResponseEntity<Resource> apiDownloadImageThumb(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_THUMBNAIL_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File thumbnailFile = fileService.getImageThumbnail(decodedImageDir, decodedImage);
        return thumbnailFile != null ? FileUtils.sendFile(thumbnailFile, thumbnailFile.getName()) : null;
    }
    
    @GetMapping(value = API_HISTOGRAM_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageHistogram(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_HISTOGRAM_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File histogramFile = fileService.getImageHistogram(decodedImageDir, decodedImage);
        return histogramFile != null ? FileUtils.sendFile(histogramFile, histogramFile.getName()) : null;
    }
    
    @GetMapping(value = API_CLOUD_COVER_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageMaskCloudCover(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_CLOUD_COVER_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File cloudCoverFile = fileService.getImageCloudCover(decodedImageDir, decodedImage);
        return cloudCoverFile != null ? FileUtils.sendFile(cloudCoverFile, cloudCoverFile.getName()) : null;
    }
    
    @GetMapping(value = API_COLOR_BALANCE_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageMaskColorBalance(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_COLOR_BALANCE_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File histogramFile = fileService.getImageColorBalance(decodedImageDir, decodedImage);
        return histogramFile != null ? FileUtils.sendFile(histogramFile, histogramFile.getName()) : null;
    }
    
    @GetMapping(value = API_NIR_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageMaskNIR(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_NIR_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File histogramFile = fileService.getImageMaskNIR(decodedImageDir, decodedImage);
        return histogramFile != null ? FileUtils.sendFile(histogramFile, histogramFile.getName()) : null;
    }
    
    @GetMapping(value = API_NDWI_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageMaskNDWI(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_NDWI_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File histogramFile = fileService.getImageMaskNDWI(decodedImageDir, decodedImage);
        return histogramFile != null ? FileUtils.sendFile(histogramFile, histogramFile.getName()) : null;
    }
    
    @GetMapping(value = API_BSI_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageMaskBSI(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_BSI_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File histogramFile = fileService.getImageMaskBSI(decodedImageDir, decodedImage);
        return histogramFile != null ? FileUtils.sendFile(histogramFile, histogramFile.getName()) : null;
    }
    
    @GetMapping(value = API_WATER_DIRECTORY_IMAGE)
    public ResponseEntity<Resource> apiDownloadImageMaskWATER(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_WATER_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final File histogramFile = fileService.getImageMaskWater(decodedImageDir, decodedImage);
        return histogramFile != null ? FileUtils.sendFile(histogramFile, histogramFile.getName()) : null;
    }
    
    @ResponseBody
    @GetMapping(value = API_BACKUP)
    public String apiBackup() {
        backupService.getBackup();
        return "ok";
    }
    
}
