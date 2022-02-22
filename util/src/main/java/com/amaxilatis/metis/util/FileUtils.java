package com.amaxilatis.metis.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class FileUtils {
    
    public static String getResultName(final File file, final int task) {
        log.debug("getResultName filename: {}, parent: {}", file.getName(), file.getParentFile().getName());
        return String.format("%s.%d.result", file.getName(), task, ".result");
    }
    
    private static String getFileParentName(final File file) {
        return file.getParentFile().getName();
    }
    
    public static File getResultFile(final String resultsDir, final File file, int task) {
        final String resultFileName = getResultName(file, task);
        final String parentDirName = getFileParentName(file);
        final File parentDir = new File(resultsDir, parentDirName);
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        return new File(parentDir, resultFileName);
    }
    
}
