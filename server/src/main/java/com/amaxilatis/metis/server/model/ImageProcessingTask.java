package com.amaxilatis.metis.server.model;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.server.config.ProcessingQueueConfiguration;
import com.amaxilatis.metis.server.db.model.Configuration;
import com.amaxilatis.metis.server.service.FileService;
import com.amaxilatis.metis.server.service.NotificationService;
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

import static com.amaxilatis.metis.server.service.FileService.CHECK_NOK;
import static com.amaxilatis.metis.server.service.FileService.CHECK_OK;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN2BitSize;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN9BlueSnr;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN5BottomClipping;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN4CloudCoverage;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN7CoefficientOfVariation;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN9ColorBalance;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN8Compression;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN9GreenSnr;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN3HasAlpha;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN6MajorBinCenterLum;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN9RedSnr;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN3SamplesPerPixel;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN3SamplesPerPixelColor;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN5TopClipping;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN1XPixelSize;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN1XPixelSizeWorld;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN1YPixelSize;
import static com.amaxilatis.metis.server.util.ResultsUtils.getN1YPixelSizeWorld;

@Slf4j
@AllArgsConstructor
@RequiredArgsConstructor
public class ImageProcessingTask implements Runnable {
    
    private final ProcessingQueueConfiguration processingQueueConfiguration;
    private FileService fileService;
    private final String outFileName;
    private String filename;
    private List<Integer> tasks;
    private Configuration configuration;
    private NotificationService notificationService;
    private Long isLastId;
    private boolean storeMasks;
    
    public void run() {
        log.info(outFileName);
        long start = System.currentTimeMillis();
        try {
            final File imageFile = new File(filename);
            log.info("parsing file {}", imageFile);
            final List<FileJobResult> results = ImageCheckerUtils.parseFile(configuration.toTestConfiguration(storeMasks), processingQueueConfiguration.getConcurrencySize(), imageFile, tasks, fileService.getResultsLocation(), fileService.getHistogramLocation(), fileService.getCloudMaskLocation(), fileService.getUncompressedLocation());
            
            final StringBuilder sb = new StringBuilder();
            sb.append("\"").append(imageFile.getName()).append("\"").append(",");
            results.forEach(result -> sb.append(String.format("\"%s\"", result.getResult() ? CHECK_OK : CHECK_NOK)).append(","));
            
            sb.append(getN1XPixelSizeWorld(results)).append(",");
            sb.append(getN1YPixelSizeWorld(results)).append(",");
            sb.append(getN1XPixelSize(results)).append(",");
            sb.append(getN1YPixelSize(results)).append(",");
            sb.append(getN2BitSize(results)).append(",");
            sb.append(getN3SamplesPerPixel(results)).append(",");
            sb.append(getN3SamplesPerPixelColor(results)).append(",");
            sb.append(getN3HasAlpha(results)).append(",");
            sb.append(getN4CloudCoverage(results)).append(",");
            sb.append(getN5TopClipping(results)).append(",");
            sb.append(getN5BottomClipping(results)).append(",");
            sb.append(getN6MajorBinCenterLum(results)).append(",");
            sb.append(getN7CoefficientOfVariation(results)).append(",");
            sb.append(getN8Compression(results)).append(",");
            sb.append(getN9ColorBalance(results)).append(",");
            sb.append(getN9RedSnr(results)).append(",");
            sb.append(getN9GreenSnr(results)).append(",");
            sb.append(getN9BlueSnr(results)).append(",");
            
            results.forEach(result -> sb.append(String.format("\"%s\"", result.getNote())).append(","));
            
            fileService.append(outFileName, sb.toString());
            
            log.info("parsed file [{}s] {} {}", ((System.currentTimeMillis() - start) / 1000), imageFile, results);
            
            if (isLastId != null) {
                notificationService.notify(isLastId);
            }
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
    
}

