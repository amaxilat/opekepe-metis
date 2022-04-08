package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.PoolInfo;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import com.amaxilatis.metis.server.service.JobService;
import com.amaxilatis.metis.server.service.ReportService;
import com.amaxilatis.metis.server.util.FileUtils;
import ij.ImagePlus;
import ij.io.FileSaver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
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

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.*;

@Slf4j
@Controller
public class ApiController extends BaseController {
    
    public ApiController(final FileService fileService, final ImageProcessingService imageProcessingService, final JobService jobService, final ReportService reportService, final MetisProperties props, final BuildProperties buildProperties) {
        super(fileService, imageProcessingService, jobService, reportService, props, buildProperties);
    }
    
    @ResponseBody
    @GetMapping(value = API_POOL, produces = MediaType.APPLICATION_JSON_VALUE)
    public PoolInfo apiPool() {
        log.info("get:{}", API_POOL);
        return imageProcessingService.getPoolInfo();
    }
    
    @ResponseBody
    @GetMapping(value = API_SCAN_IMAGES, produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> scanImages() {
        log.info("get:{}", API_SCAN_IMAGES);
        fileService.updateImageDirs();
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
    
    @GetMapping(value = API_IMAGE_DIRECTORY_IMAGE_DOWNLOAD)
    public ResponseEntity<InputStreamResource> apiDownloadImage(final HttpServletResponse response, @PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) throws IOException {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_IMAGE_DIRECTORY_IMAGE_DOWNLOAD, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        FileUtils.sendFile(response, new File(new File(fileService.getFilesLocation(), decodedImageDir), decodedImage), decodedImage);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping(value = API_THUMBNAIL_DIRECTORY_IMAGE)
    public ResponseEntity<InputStreamResource> apiDownloadImageThumb(final HttpServletResponse response, @PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) throws IOException {
        log.info("get:{}, imageDirectoryHash:{}, imageHash:{}", API_THUMBNAIL_DIRECTORY_IMAGE, imageDirectoryHash, imageHash);
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + decodedImage);
        log.info("[thumb] " + fileService.getFilesLocation() + "/" + decodedImageDir + "/" + decodedImage);
        final ImagePlus ip = FileUtils.makeThumbnail(new ImagePlus(fileService.getFilesLocation() + "/" + decodedImageDir + "/" + decodedImage), 300);
        final File thumbnailFile = new File(props.getResultsLocation() + "/" + decodedImage + ".jpg");
        if (thumbnailFile.exists()) {
            FileUtils.sendFile(response, thumbnailFile, thumbnailFile.getName());
        } else {
            log.info("[thumb:1] " + thumbnailFile.getAbsolutePath());
            new FileSaver(ip).saveAsJpeg(thumbnailFile.getAbsolutePath());
            FileUtils.sendFile(response, thumbnailFile, thumbnailFile.getName());
            thumbnailFile.deleteOnExit();
            fileService.deleteFile(thumbnailFile.getAbsolutePath());
        }
        return ResponseEntity.ok().build();
    }
    
}
