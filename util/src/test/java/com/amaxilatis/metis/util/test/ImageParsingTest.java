package com.amaxilatis.metis.util.test;

import com.amaxilatis.metis.util.Utils;
import com.amaxilatis.metis.util.model.FileJobResult;
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
                results.addAll(Utils.parseDir(directory1T, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(Utils.parseFile(directory1T, tasks));
            }
            for (final FileJobResult result : results) {
                Assertions.assertEquals(result.getTask(), tasks.get(0));
                Assertions.assertTrue(result.getResult());
            }
        } catch (IOException | TikaException | SAXException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Test
    void testN2_false() {
        tasks.add(2);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory2F.isDirectory()) {
                results.addAll(Utils.parseDir(directory2F, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(Utils.parseFile(directory2F, tasks));
            }
            for (final FileJobResult result : results) {
                Assertions.assertEquals(result.getTask(), tasks.get(0));
                Assertions.assertTrue(result.getResult());
            }
        } catch (IOException | TikaException | SAXException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Test
    void testN3_true() {
        tasks.add(3);
        try {
            final List<FileJobResult> results = new ArrayList<>();
            if (directory3T.isDirectory()) {
                results.addAll(Utils.parseDir(directory3T, tasks));
            } else {
                log.warn("Input not a directory");
                results.addAll(Utils.parseFile(directory3T, tasks));
            }
            for (final FileJobResult result : results) {
                Assertions.assertEquals(result.getTask(), tasks.get(0));
                Assertions.assertTrue(result.getResult());
            }
        } catch (IOException | TikaException | SAXException e) {
            log.error(e.getMessage(), e);
        }
    }
}
