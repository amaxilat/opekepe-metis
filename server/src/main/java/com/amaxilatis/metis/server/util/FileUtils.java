package com.amaxilatis.metis.server.util;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.MedianCut;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
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
    
    public static ImagePlus makeThumbnail(final ImagePlus imp, final int thumbnailWidth) {
        if (imp == null) {
            return null;
        }
        ImageProcessor ip = imp.getProcessor();
        final int width = ip.getWidth();
        final int height = ip.getHeight();
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
