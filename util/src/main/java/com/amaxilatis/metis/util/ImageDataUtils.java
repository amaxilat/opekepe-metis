package com.amaxilatis.metis.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
public class ImageDataUtils {
    
    /**
     * Extracts a tile from an image starting from 0,0 coordinates.
     *
     * @param image           the base image
     * @param width           the width of the needed tile
     * @param height          the height of the needed tile
     * @param components      the number of components in the image
     * @param tileWidthIndex  the width index of the tile
     * @param tileHeightIndex the height index of the tile
     * @return the tile's data as an int array.
     */
    public static int[] getImageTileData(final BufferedImage image, final int width, final int height, final int components, int tileWidthIndex, int tileHeightIndex) {
        final int size = width * height * components;
        int[] dnValues = new int[size];
        image.getData().getPixels(tileWidthIndex * width, tileHeightIndex * height, width, height, dnValues);
        return dnValues;
    }
    
    /**
     * Extracts a tile from an image starting from the given coordinates.
     *
     * @param image       the base image
     * @param width       the width of the needed tile
     * @param height      the height of the needed tile
     * @param components  the number of components in the image
     * @param startWidth  the starting width coordinate
     * @param startHeight the starting height coordinate
     * @return the tile's data as an int array.
     */
    public static int[] getImageTileDataFromCoordinates(final BufferedImage image, final int width, final int height, final int components, final int startWidth, final int startHeight) {
        final int size = width * height * components;
        int[] dnValues = new int[size];
        image.getData().getPixels(startWidth, startHeight, width, height, dnValues);
        return dnValues;
    }
    
    /**
     * Checks if the tile is empty, with full black or full white pixels.
     *
     * @param pixelValues the pixel values
     * @return true if all the pixels are empty and false if at least one is not.
     */
    public static boolean isEmptyTile(final int maxValue, final int[] pixelValues) {
        for (int i = 0; i < pixelValues.length; i += 4) {
            if (isValidPixel(maxValue, pixelValues[i], pixelValues[i + 1], pixelValues[i + 2], pixelValues[i + 3])) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the pixel's value is valid (non-white and non-black).
     *
     * @param pixelColor the pixel's color
     * @return true if the pixel is non-white and non-black, false else.
     */
    public static boolean isValidPixel(final int maxValue, final Color pixelColor) {
        return isValidPixel(maxValue, pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue(), pixelColor.getAlpha());
    }
    
    /**
     * Checks if the pixel's value is valid (non-white and non-black).
     *
     * @param r   the red component of the pixel
     * @param g   the green component of the pixel
     * @param b   the blue component of the pixel
     * @param nir the nir component of the pixel
     * @return true if the pixel is non-white and non-black, false else.
     */
    public static boolean isValidPixel(final int maxValue, final int r, final int g, final int b, final int nir) {
        final int maxThresh = maxValue - 1;
        return (maxThresh != r || maxThresh != g || maxThresh != b) && (0 != r || 0 != g || 0 != b);
        //removed nir
    }
}
