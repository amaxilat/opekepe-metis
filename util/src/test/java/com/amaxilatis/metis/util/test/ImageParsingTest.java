package com.amaxilatis.metis.util.test;

import com.amaxilatis.metis.model.FileJobResult;
import com.amaxilatis.metis.model.TestConfiguration;
import com.amaxilatis.metis.util.ImageCheckerUtils;
import com.drew.imaging.ImageProcessingException;
import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageParsingTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageParsingTest.class);
    
    final File directory1T = new File("..\\dataset\\dataset1T\\");
    final File directory3T = new File("..\\dataset\\dataset3T\\");
    final File directory2F = new File("..\\dataset\\dataset2F\\");
    final File directory6F = new File("..\\dataset\\dataset6F\\");
    final File directory8 = new File("..\\dataset\\dataset8\\");
    final File directory4 = new File("..\\dataset\\dataset4\\0456039030.tif");
    private static List<Integer> tasks;
    private final TestConfiguration testConfiguration = new TestConfiguration(0.5, 8, 4, 2.0, 0.5, 0.1, 0.2, 2.0, 5.0, true);
    
    @BeforeAll
    static void beforeAll() {
        tasks = new ArrayList<>();
    }
    
    @BeforeEach
    void beforeEach() {
        tasks.clear();
    }
    
    @Test
    void testN1_true() {
        tasks.add(1);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory1T.isDirectory()) {
                results.addAll(ImageCheckerUtils.parseDir(testConfiguration, directory1T, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(testConfiguration, 1, directory1T, tasks, null, null, null, null));
            }
            for (final FileJobResult result : results) {
                Assertions.assertEquals(result.getTask(), tasks.get(0));
                Assertions.assertTrue(result.getResult());
            }
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Test
    void testN2_false() {
        tasks.add(2);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory2F.isDirectory()) {
                results.addAll(ImageCheckerUtils.parseDir(testConfiguration, directory2F, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(testConfiguration, 1, directory2F, tasks, null, null, null, null));
            }
            for (final FileJobResult result : results) {
                Assertions.assertEquals(result.getTask(), tasks.get(0));
                Assertions.assertTrue(result.getResult());
            }
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Test
    void testN3_true() {
        tasks.add(3);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory3T.isDirectory()) {
                results.addAll(ImageCheckerUtils.parseDir(testConfiguration, directory3T, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(testConfiguration, 1, directory3T, tasks, null, null, null, null));
            }
            for (final FileJobResult result : results) {
                Assertions.assertEquals(result.getTask(), tasks.get(0));
                Assertions.assertTrue(result.getResult());
            }
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Test
    void testN4_true() {
        tasks.add(4);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory4.isDirectory()) {
                results.addAll(ImageCheckerUtils.parseDir(testConfiguration, directory4, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(testConfiguration, 1, directory4, tasks, null, null, directory4.getParent(), null));
            }
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Test
    void testN6_false() {
        tasks.add(5);
        tasks.add(6);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory3T.isDirectory()) {
                results.addAll(ImageCheckerUtils.parseDir(testConfiguration, directory6F, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(testConfiguration, 1, directory6F, tasks, null, null, null, null));
            }
            for (final FileJobResult result : results) {
                log.info(result.toString());
            }
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Test
    void testN8_false() {
        tasks.add(8);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory8.isDirectory()) {
                results.addAll(ImageCheckerUtils.parseDir(testConfiguration, directory8, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(testConfiguration, 1, directory8, tasks, null, null, null, null));
            }
            for (final FileJobResult result : results) {
                log.info(result.toString());
            }
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
}
