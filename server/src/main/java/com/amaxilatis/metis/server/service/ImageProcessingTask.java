package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.rabbit.FileService;
import com.amaxilatis.metis.util.Utils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class ImageProcessingTask implements Runnable {
    
    private FileService fileService;
    private final String outFileName;
    private String name;
    private List<Integer> tasks;
    
    public void run() {
        log.info(outFileName);
        long start = System.currentTimeMillis();
        try {
            File file = new File(name);
            log.info("parsing file {}", file);
            final List<FileJobResult> results = Utils.parseFile(file, tasks);
            
            final StringBuilder sb = new StringBuilder();
            sb.append(file.getName()).append(",");
            results.forEach(result -> sb.append(String.format("\"%s\"", result.getResult() ? "ΟΚ" : "ΛΑΘΟΣ")).append(","));
            results.forEach(result -> sb.append(String.format("\"%s\"", result.getNote())).append(","));
            
            fileService.append(outFileName, sb.toString());
            
            //append2Excel(sheet, results);
            log.info("parsed file [{}s] {} {}", ((System.currentTimeMillis() - start) / 1000), file, results);
        } catch (IOException | TikaException | SAXException e) {
            log.error(e.getMessage(), e);
        }
    }
    
}
