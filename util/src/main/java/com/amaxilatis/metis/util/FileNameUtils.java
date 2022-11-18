package com.amaxilatis.metis.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class FileNameUtils {
    
    /**
     * Get the name of the result's file.
     *
     * @param file the file of the image.
     * @param task the task for the check performed.
     * @return the name of the result's file.
     */
    public static String getResultName(final File file, final int task) {
        return String.format("%s.%d.result", file.getName(), task);
    }
    
    /**
     * Get the directory of the image.
     *
     * @param file the file of the image.
     * @return the name of the directory of the image.
     */
    public static String getFileParentName(final File file) {
        return file.getParentFile().getName();
    }
    
    /**
     * Get the name of the result file for the image and the specific task performed.
     *
     * @param resultsDir the directory where all results are stored.
     * @param file       the file of the image.
     * @param task       the task for the check performed.
     * @return the name of the result file.
     */
    public static File getResultFile(final String resultsDir, final File file, int task) {
        final String resultFileName = getResultName(file, task);
        final String parentDirName = getFileParentName(file);
        final File parentDir = new File(resultsDir, parentDirName);
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        return new File(parentDir, resultFileName);
    }
    
    /**
     * Returns the full path to the image's file.
     *
     * @param location the location where images are stored.
     * @param dir      the directory of the image.
     * @param name     the name of the image.
     * @return the full  path to the image's file.
     */
    public static String getImageFilename(final String location, final String dir, final String name) {
        return String.format("%s/%s/%s", location, dir, name);
    }
    
    /**
     * Returns the full path to the image's uncompressed file.
     *
     * @param location the location where uncompressed images are stored.
     * @param dir      the directory of the image.
     * @param name     the name of the image.
     * @return the full  path to the image's uncompressed file.
     */
    public static String getImageUncompressedFilename(final String location, final String dir, final String name) {
        return String.format("%s/%s/%s", location, dir, name);
    }
    
    /**
     * Returns the full path to the image's thumbnail.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's thumbnail file.
     */
    public static String getImageThumbnailFilename(final String location, final String dir, final String name) {
        return String.format("%s/%s/%s", location, dir, name + ".jpg");
    }
    
    /**
     * Returns the full path to the image's histogram.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's histogram file.
     */
    public static String getImageHistogramFilename(final String location, final String dir, final String name) {
        return String.format("%s/%s/%s", location, dir, name + ".png");
    }
    
    /**
     * Returns the full path to the image's cloud coverage mask.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's cloud coverage mask file.
     */
    public static String getImageCloudCoverMaskFilename(final String location, final String dir, final String name) {
        return String.format("%s/%s/%s", location, dir, name + ".mask.png");
    }
    
    /**
     * Returns the full path to the image's color balance mask.
     *
     * @param dir  the directory of the image.
     * @param name the name of the image.
     * @return the full  path to the image's color balance mask file.
     */
    public static String getImageColorBalanceMaskFilename(final String location, final String dir, final String name) {
        return String.format("%s/%s/%s", location, dir, name + ".cbmask.png");
    }
    
    
    public static String extractImageNameFromResult(final String name) {
        final String[] parts = name.split("\\.");
        return parts[0] + "." + parts[1];
    }
}
