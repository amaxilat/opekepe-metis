package com.amaxilatis.metis.server.util;

import com.amaxilatis.metis.model.FileJobResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class ResultsUtils {
    
    public static List<String> resultsTitles = List.of("Μέγεθος Pixel X worldFile", "Μέγεθος Pixel Y worldFile", "Μέγεθος Pixel X exif", "Μέγεθος Pixel Y exif", "bits ανά Pixel", "Κανάλια", "Χρώματα", "NIR", "Νεφοκάλυψη", "Clipping τελευταία 5 bins", "Clipping πρώτα 5 bins", "Κέντρο Ιστογράμματος", "Συντελεστής Μεταβλητότητας", "Συμπίεση", "Ισορροπία Χρώματος", "Θόρυβος Κόκκινο", "Θόρυβος Πράσινο", "Θόρυβος Μπλε");
    
    public static FileJobResult getTaskById(final Iterable<FileJobResult> results, int id) {
        for (final FileJobResult result : results) {
            if (result.getTask() == id) {
                return result;
            }
        }
        return null;
    }
    
    public static String getN1XPixelSizeWorld(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 1);
        if (result != null) {
            return String.valueOf(result.getN1XPixelSizeWorld());
        }
        return "";
    }
    
    public static String getN1YPixelSizeWorld(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 1);
        if (result != null) {
            return String.valueOf(result.getN1YPixelSizeWorld());
        }
        return "";
    }
    
    public static String getN1XPixelSize(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 1);
        if (result != null) {
            return String.valueOf(result.getN1XPixelSize());
        }
        return "";
    }
    
    public static String getN1YPixelSize(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 1);
        if (result != null) {
            return String.valueOf(result.getN1YPixelSize());
        }
        return "";
    }
    
    public static String getN2BitSize(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 2);
        if (result != null) {
            return String.valueOf(result.getN2BitSize());
        }
        return "";
    }
    
    public static String getN3SamplesPerPixel(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 3);
        if (result != null) {
            return String.valueOf(result.getN3SamplesPerPixel());
        }
        return "";
    }
    
    public static String getN3SamplesPerPixelColor(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 3);
        if (result != null) {
            return String.valueOf(result.getN3SamplesPerPixelColor());
        }
        return "";
    }
    
    public static String getN3HasAlpha(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 3);
        if (result != null) {
            return String.valueOf(result.getN3HasAlpha() ? "Ναι" : "Όχι");
        }
        return "";
    }
    
    public static String getN4CloudCoverage(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 4);
        if (result != null) {
            return String.valueOf(result.getN4CloudCoverage());
        }
        return "";
    }
    
    public static String getN5TopClipping(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 5);
        if (result != null) {
            return String.valueOf(result.getB5TopClipping());
        }
        return "";
    }
    
    public static String getN5BottomClipping(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 5);
        if (result != null) {
            return String.valueOf(result.getN5BottomClipping());
        }
        return "";
    }
    
    public static String getN6MajorBinCenterLum(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 6);
        if (result != null) {
            return String.valueOf(result.getN6LumHistCenter());
        }
        return "";
    }
    
    public static String getN7CoefficientOfVariation(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 7);
        if (result != null) {
            return String.valueOf(result.getN7CoefficientOfVariation());
        }
        return "";
    }
    
    public static String getN8Compression(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 8);
        if (result != null) {
            return String.valueOf(result.getN8Compression());
        }
        return "";
    }
    
    public static String getN9ColorBalance(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 9);
        if (result != null) {
            return String.valueOf(result.getN9ColorBalance());
        }
        return "";
    }
    
    public static String getN9RedSnr(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 9);
        if (result != null) {
            return String.valueOf(result.getN9RedSnr());
        }
        return "";
    }
    
    public static String getN9GreenSnr(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 9);
        if (result != null) {
            return String.valueOf(result.getN9GreenSnr());
        }
        return "";
    }
    
    public static String getN9BlueSnr(final Iterable<FileJobResult> results) {
        FileJobResult result = getTaskById(results, 9);
        if (result != null) {
            return String.valueOf(result.getN9BlueSnr());
        }
        return "";
    }
}
