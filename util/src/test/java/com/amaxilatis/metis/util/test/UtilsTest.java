package com.amaxilatis.metis.util.test;

import com.amaxilatis.metis.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

@Slf4j
public class UtilsTest {
    
    public static void main(String... args) {
        try {
            final File directory = new File("C:\\Users\\qopbo\\IdeaProjects\\opekepe-metis\\wetransfer_2021-11-04_0714\\");
            if (directory.isDirectory()) {
                Utils.parse(directory);
            } else {
                log.error("Input not a directory");
            }
        } catch (IOException | TikaException | SAXException e) {
            log.error(e.getMessage(), e);
        }
    }
}
