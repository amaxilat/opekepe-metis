package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.PoolInfo;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.SortedSet;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ApiController {
    
    private final ImageProcessingService imageProcessingService;
    private final FileService fileService;
    
    @ResponseBody
    @GetMapping(value = API_POOL, produces = MediaType.APPLICATION_JSON_VALUE)
    public PoolInfo apiPool() {
        return imageProcessingService.getPoolInfo();
    }
    
    @ResponseBody
    @GetMapping(value = API_SCAN_IMAGES, produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> scanImages() {
        fileService.updateImageDirs();
        return apiImages();
    }
    
    @ResponseBody
    @GetMapping(value = API_IMAGE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> apiImages() {
        return fileService.getImagesDirs();
    }
    
    @ResponseBody
    @GetMapping(value = API_IMAGE_DIRECTORY, produces = MediaType.APPLICATION_JSON_VALUE)
    public SortedSet<ImageFileInfo> apiImagesInDirectory(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash) {
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        return fileService.listImages(decodedImageDir);
    }
    
    @ResponseBody
    @GetMapping(value = API_IMAGE_DIRECTORY_IMAGE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileJobResult> apiImages(@PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) {
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        return fileService.getImageResults(decodedImageDir, decodedImage);
    }
    
    @GetMapping(value = API_IMAGE_DIRECTORY_IMAGE_DOWNLOAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InputStreamResource> apiDownloadImage(final HttpServletResponse response, @PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) throws IOException {
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        final InputStream resource = new FileInputStream(new File(new File(fileService.getFilesLocation(), decodedImageDir), decodedImage));
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + decodedImage);
        IOUtils.copy(resource, response.getOutputStream());
        response.flushBuffer();
        return ResponseEntity.ok().build();
    }
}
