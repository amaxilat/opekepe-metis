package com.amaxilatis.metis.util;

import com.amaxilatis.metis.model.WorldFile;
import com.amaxilatis.metis.model.WorldFileResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WorldFileUtils {
    
    public static WorldFileResult evaluateWorldFile(final WorldFile worldFile) {
        final int xCenterDecimal = ((int) (worldFile.getXCenter() * 100) % 100);
        final int yCenterDecimal = ((int) (worldFile.getYCenter() * 100) % 100);
        final String note = String.format("%.1f,%.1f,%.1f,%.1f,%d,%d", worldFile.getXPixelSize(), worldFile.getXRotation(), worldFile.getYRotation(), worldFile.getYPixelSize(), xCenterDecimal, yCenterDecimal);
        return new WorldFileResult(note, worldFile.getXPixelSize() == 0.5 && worldFile.getXRotation() == 0 && worldFile.getYRotation() == 0 && worldFile.getYPixelSize() == -0.5 && xCenterDecimal == 25 && yCenterDecimal == 75);
    }
    
    public static File getWorldFile(final File file) {
        final File parentFile = file.getParentFile();
        if (file.getName().endsWith(".tif")) {
            return new File(parentFile, file.getName().replaceAll("tif$", "tfw"));
        } else if (file.getName().endsWith(".jpg")) {
            return new File(parentFile, file.getName().replaceAll("jpg$", "jgw"));
        } else if (file.getName().endsWith(".ecw")) {
            return new File(parentFile, file.getName().replaceAll("ecw$", "eww"));
        } else if (file.getName().endsWith(".jp2")) {
            return new File(parentFile, file.getName().replaceAll("jp2$", "j2w"));
        } else {
            return null;
        }
    }
    
    public static WorldFile parseWorldFile(final File wFile) {
        final List<Object> lines = new ArrayList<>();
        try (final BufferedReader br = new BufferedReader(new FileReader(wFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return WorldFile.builder().xPixelSize(Double.parseDouble((String) lines.get(0))).yRotation(Double.parseDouble((String) lines.get(1))).xRotation(Double.parseDouble((String) lines.get(2))).yPixelSize(Double.parseDouble((String) lines.get(3))).xCenter(Double.parseDouble((String) lines.get(4))).yCenter(Double.parseDouble((String) lines.get(5))).build();
    }
    
}
