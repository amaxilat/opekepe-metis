package com.amaxilatis.metis.util.test;

import com.amaxilatis.metis.model.WorldFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.amaxilatis.metis.util.WorldFileUtils.getWorldFile;
import static com.amaxilatis.metis.util.WorldFileUtils.parseWorldFile;

public class WorldFileParsingTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorldFileParsingTest.class);
    
    @BeforeAll
    static void beforeAll() {
        log.debug("init WorldFileParsingTest");
    }
    
    @Test
    void testGetWorldFile_1() {
        final File f = new File("0452039030.tif");
        final File worldFile = getWorldFile(f);
        Assertions.assertNotNull(worldFile);
        Assertions.assertEquals("0452039030.tfw", worldFile.getName());
    }
    
    @Test
    void testGetWorldFile_2() {
        final File f = new File("0452039030.jpg");
        final File worldFile = getWorldFile(f);
        Assertions.assertNotNull(worldFile);
        Assertions.assertEquals("0452039030.jgw", worldFile.getName());
    }
    
    @Test
    void testGetWorldFile_3() {
        final File f = new File("0452039030.ecw");
        final File worldFile = getWorldFile(f);
        Assertions.assertNotNull(worldFile);
        Assertions.assertEquals("0452039030.eww", worldFile.getName());
    }
    
    @Test
    void testGetWorldFile_4() {
        final File f = new File("0452039030.jp2");
        final File worldFile = getWorldFile(f);
        Assertions.assertNotNull(worldFile);
        Assertions.assertEquals("0452039030.j2w", worldFile.getName());
    }
    
    @Test
    void testParseWorldFile() {
        final File f = new File("0452039030.tfw");
        final WorldFile worldFile = parseWorldFile(f);
        Assertions.assertEquals(0.5, worldFile.getXPixelSize());
        Assertions.assertEquals(0, worldFile.getYRotation());
        Assertions.assertEquals(0, worldFile.getXRotation());
        Assertions.assertEquals(-0.5, worldFile.getYPixelSize());
        Assertions.assertEquals(452000.25, worldFile.getXCenter());
        Assertions.assertEquals(3905999.75, worldFile.getYCenter());
    }
}
