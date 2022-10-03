package com.amaxilatis.metis.server.util;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.MedianCut;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FileUtils {
    
    
    public static ResponseEntity<Resource> sendFile(final HttpServletResponse response, final File file, final String decodedImage) {
        final Path path = Paths.get(file.getPath());
        Resource resource = null;
        try {
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + decodedImage + "\"").body(resource);
    }
    
    public static void makeThumbnail(final File input, final File output, final int width, final int height) throws IOException {
        if (input.getName().endsWith("jp2")) {
        } else {
            Thumbnails.of(input).size(width, height).toFile(output);
        }
    }
    
    
    private static ImageProcessor reduceColors(ImageProcessor ip, final int nColors) {
        if (ip instanceof ByteProcessor && nColors == 256) {
            return ip;
        }
        ip = ip.convertToRGB();
        final MedianCut mc = new MedianCut((int[]) ip.getPixels(), ip.getWidth(), ip.getHeight());
        final Image img = mc.convert(nColors);
        return new ByteProcessor(img);
    }
}
