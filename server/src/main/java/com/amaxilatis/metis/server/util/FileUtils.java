package com.amaxilatis.metis.server.util;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.MedianCut;
import ij.process.ShortProcessor;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    
    
    public static void sendFile(final HttpServletResponse response, final File file, final String decodedImage) throws IOException {
        final InputStream resource = new FileInputStream(file);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + decodedImage);
        IOUtils.copy(resource, response.getOutputStream());
        response.flushBuffer();
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
