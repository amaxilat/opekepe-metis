package com.amaxilatis.metis.util.test;

import com.amaxilatis.metis.util.ImageCheckerUtils;
import com.amaxilatis.metis.model.FileJobResult;
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
    private static List<Integer> tasks;
    
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
                results.addAll(ImageCheckerUtils.parseDir(directory1T, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(directory1T, tasks, null));
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
                results.addAll(ImageCheckerUtils.parseDir(directory2F, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(directory2F, tasks, null));
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
                results.addAll(ImageCheckerUtils.parseDir(directory3T, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(directory3T, tasks, null));
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
    void testN6_false() {
        tasks.add(5);
        tasks.add(6);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory3T.isDirectory()) {
                results.addAll(ImageCheckerUtils.parseDir(directory6F, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(ImageCheckerUtils.parseFile(directory6F, tasks, null));
            }
            for (final FileJobResult result : results) {
                log.info(result.toString());
                //Assertions.assertEquals(result.getTask(), tasks.get(0));
                //Assertions.assertFalse(result.getResult());
            }
        } catch (IOException | TikaException | SAXException | ImageProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
}
