package com.amaxilatis.metis.util;

import lombok.extern.slf4j.Slf4j;

import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_CCITT_RLE;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_CCITT_T_4;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_CCITT_T_6;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_DEFLATE;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_LZW;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_NONE;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_PACKBITS;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.COMPRESSION_ZLIB;

@Slf4j
public class CompressionUtils {
    
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
                return "";
            default:
                return String.valueOf(compressionExifValue);
        }
    }
    
    public static boolean isLossless(final int compressionExifValue) {
        return compressionExifValue == COMPRESSION_NONE || compressionExifValue == COMPRESSION_CCITT_RLE || compressionExifValue == COMPRESSION_CCITT_T_4 || compressionExifValue == COMPRESSION_CCITT_T_6 || compressionExifValue == COMPRESSION_LZW || compressionExifValue == COMPRESSION_ZLIB || compressionExifValue == COMPRESSION_PACKBITS || compressionExifValue == COMPRESSION_DEFLATE;
    }
}
