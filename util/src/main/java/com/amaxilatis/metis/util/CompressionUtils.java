package com.amaxilatis.metis.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_CCITT_RLE;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_CCITT_T_4;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_CCITT_T_6;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_DEFLATE;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_JPEG;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_LZW;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_NONE;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_PACKBITS;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_ZLIB;

@Slf4j
public class CompressionUtils {
    
    private static final List<Integer> LOSSLESS_COMPRESSION_TYPES = Arrays.asList(COMPRESSION_NONE, COMPRESSION_CCITT_RLE, COMPRESSION_CCITT_T_4, COMPRESSION_CCITT_T_6, COMPRESSION_LZW, COMPRESSION_ZLIB, COMPRESSION_PACKBITS, COMPRESSION_DEFLATE);
    
    /**
     * Convert the exif value describing the compression to a text representation.
     *
     * @param compressionExifValue the exif value for the compression key
     * @return the text representation of the exif value.
     */
    public static String toText(final int compressionExifValue) {
        switch (compressionExifValue) {
            case COMPRESSION_NONE:
                return "Χωρίς";
            case COMPRESSION_CCITT_RLE:
                return "CCITT_RLE";
            case COMPRESSION_CCITT_T_4:
                return "CCITT_T_4";
            case COMPRESSION_CCITT_T_6:
                return "CCITT_T_6";
            case COMPRESSION_LZW:
                return "LZW";
            case COMPRESSION_ZLIB:
                return "ZLIB";
            case COMPRESSION_PACKBITS:
                return "PACKBITS";
            case COMPRESSION_DEFLATE:
                return "DEFLATE";
            case COMPRESSION_JPEG:
                return "JPEG";
            default:
                return String.valueOf(compressionExifValue);
        }
    }
    
    /**
     * Checks if the exif value describes a compressed type.
     *
     * @param compressionExifValue the exif value for the compression key
     * @return true of the exif value describes compression, false if not.
     */
    public static boolean isCompressed(final int compressionExifValue) {
        return compressionExifValue != COMPRESSION_NONE;
    }
    
    /**
     * Checks if the exif value describes a lossless compression.
     *
     * @param compressionExifValue the exif value for the compression key
     * @return true if the compression is lossless, false otherwise.
     */
    public static boolean isLossless(final int compressionExifValue) {
        return LOSSLESS_COMPRESSION_TYPES.contains(compressionExifValue);
    }
}
