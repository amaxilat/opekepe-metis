package com.amaxilatis.metis.server.model;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.util.ImageCheckerUtils;
import com.drew.imaging.ImageProcessingException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@AllArgsConstructor
@RequiredArgsConstructor
public class ImageProcessingTask implements Runnable {
    
    private FileService fileService;
    private final String outFileName;
    private String filename;
    private List<Integer> tasks;
    
    public void run() {
        log.info(outFileName);
        long start = System.currentTimeMillis();
        try {
            final File imageFile = new File(filename);
            log.info("parsing file {}", imageFile);
            final List<FileJobResult> results = ImageCheckerUtils.parseFile(imageFile, tasks, fileService.getResultsLocation());
            
            final StringBuilder sb = new StringBuilder();
            sb.append("\"").append(imageFile.getName()).append("\"").append(",");
            results.forEach(result -> sb.append(String.format("\"%s\"", result.getResult() ? "ΟΚ" : "ΛΑΘΟΣ")).append(","));
            results.forEach(result -> sb.append(String.format("\"%s\"", result.getNote())).append(","));
            
            fileService.append(outFileName, sb.toString());
            
            log.info("parsed file [{}s] {} {}", ((System.currentTimeMillis() - start) / 1000), imageFile, results);
            log.info("processing complete {}", System.currentTimeMillis());
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
    
}
