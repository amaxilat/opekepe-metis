package com.amaxilatis.metis.server.model;

import com.amaxilatis.metis.server.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ImageThumbnailTask implements Runnable {
    
    private final FileService fileService;
    private final String imagesDirectory;
    private final String image;
    
    public void run() {
        long start = System.currentTimeMillis();
        fileService.getImageThumbnail(imagesDirectory, image);
        log.info("parsed file [{}s] {}", ((System.currentTimeMillis() - start) / 1000), image);
    }
    
}

