package com.amaxilatis.metis.server.web.controller;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.config.MetisProperties;
import com.amaxilatis.metis.server.model.ImageFileInfo;
import com.amaxilatis.metis.server.model.PoolInfo;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.ImageProcessingService;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.MedianCut;
import ij.process.ShortProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;

import static com.amaxilatis.metis.server.web.controller.ApiRoutes.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ApiController {
    
    private final ImageProcessingService imageProcessingService;
    private final MetisProperties props;
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
    
    @GetMapping(value = API_IMAGE_DIRECTORY_IMAGE_DOWNLOAD)
    public ResponseEntity<InputStreamResource> apiDownloadImage(final HttpServletResponse response, @PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) throws IOException {
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        sendFile(response, new File(new File(fileService.getFilesLocation(), decodedImageDir), decodedImage), decodedImage);
        return ResponseEntity.ok().build();
    }
    
    private void sendFile(final HttpServletResponse response, final File file, String decodedImage) throws IOException {
        final InputStream resource = new FileInputStream(file);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + decodedImage);
        IOUtils.copy(resource, response.getOutputStream());
        response.flushBuffer();
    }
    
    @GetMapping(value = API_THUMBNAIL_DIRECTORY_IMAGE)
    public ResponseEntity<InputStreamResource> apiDownloadImageThumb(final HttpServletResponse response, @PathVariable(IMAGE_DIR_HASH) final String imageDirectoryHash, @PathVariable(IMAGE_HASH) final String imageHash) throws IOException {
        final String decodedImageDir = fileService.getStringFromHash(imageDirectoryHash);
        final String decodedImage = fileService.getStringFromHash(imageHash);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + decodedImage);
        log.info("[thumb] " + fileService.getFilesLocation() + "/" + decodedImageDir + "/" + decodedImage);
        final ImagePlus ip = makeThumbnail(new ImagePlus(fileService.getFilesLocation() + "/" + decodedImageDir + "/" + decodedImage), 300);
        final File thumbnailFile = new File(props.getResultsLocation() + "/" + decodedImage + ".jpg");
        if (thumbnailFile.exists()) {
            sendFile(response, thumbnailFile, thumbnailFile.getName());
        } else {
            log.info("[thumb:1] " + thumbnailFile.getAbsolutePath());
            new FileSaver(ip).saveAsJpeg(thumbnailFile.getAbsolutePath());
            sendFile(response, thumbnailFile, thumbnailFile.getName());
            thumbnailFile.deleteOnExit();
            fileService.deleteFile(thumbnailFile.getAbsolutePath());
        }
        return ResponseEntity.ok().build();
    }
    
    
    ImagePlus makeThumbnail(ImagePlus imp, int thumbnailWidth) {
        if (imp == null)
            return null;
        ImageProcessor ip = imp.getProcessor();
        int width = ip.getWidth();
        int height = ip.getHeight();
        if (imp.getType() == ImagePlus.COLOR_256)
            ip = ip.convertToRGB();
        ip.smooth();
        ip.setInterpolate(true);
        ImageProcessor ip2 = ip.resize(thumbnailWidth, thumbnailWidth * height / width);
        ip.reset();
        if (ip2 instanceof ShortProcessor || ip2 instanceof FloatProcessor)
            ip2 = ip2.convertToByte(true);
        ip2 = reduceColors(ip2, 256);
        return new ImagePlus("Thumbnail", ip2);
    }
    
    ImageProcessor reduceColors(ImageProcessor ip, int nColors) {
        if (ip instanceof ByteProcessor && nColors == 256)
            return ip;
        ip = ip.convertToRGB();
        MedianCut mc = new MedianCut((int[]) ip.getPixels(), ip.getWidth(), ip.getHeight());
        Image img = mc.convert(nColors);
        return (new ByteProcessor(img));
    }
    
}
