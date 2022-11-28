package com.amaxilatis.metis.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
public class CloudUtils {
    
    public static final int WHITE_RGB = Color.white.getRGB(); // Color white
    public static final int BLACK_RGB = Color.black.getRGB(); // Color black
    public static final int GRAY_RGB = Color.gray.getRGB(); // Color gray
    public static final int LIGHT_GRAY_RGB = Color.lightGray.getRGB(); // Color gray
    
    /**
     * Cleans up the image mask based on the percentage of cloud pixels found inside each tile of the provided size.
     *
     * @param image               the object containing the image mask
     * @param width               the width of the image
     * @param height              the height of the image
     * @param tileSize            the size of the tile
     * @param percentageThreshold the top threshold to consider a cloudy tile invalid (0-100)
     * @return the number of the removed cloud pixels.
     */
    public static int cleanupCloudsBasedOnTiles(final BufferedImage image, final int width, final int height, final int tileSize, final int percentageThreshold) {
        int removedPixels = 0;
        final int widthTiles = width / tileSize;
        final int heightTiles = height / tileSize;
        final int tileArea = tileSize * tileSize;
        final int[] maskValues = new int[tileArea];
        for (int w = 0; w < widthTiles; w++) {
            for (int h = 0; h < heightTiles; h++) {
                image.getRGB(w * tileSize, h * tileSize, tileSize, tileSize, maskValues, 0, tileSize);
                double count = 0;
                for (int maskValue : maskValues) {
                    if (maskValue == WHITE_RGB) {
                        count++;
                    }
                }
                final double percentage = (count / (tileArea)) * tileSize;
                if (count > 0 && percentage <= percentageThreshold) {
                    //if less than 1% of the tile are clouds, then probably no cloud in the tile
                    for (int i = 0; i < tileSize; i++) {
                        for (int j = 0; j < tileSize; j++) {
                            if (image.getRGB(w * tileSize + i, h * tileSize + j) == WHITE_RGB) {
                                image.setRGB(w * tileSize + i, h * tileSize + j, LIGHT_GRAY_RGB);
                                removedPixels++;
                            }
                        }
                    }
                }
            }
        }
        return removedPixels;
    }
    
    /**
     * Cleans up the image mask based on the number of nearby pixels belonging to a cloud, used to remove rogue cloud pixels that are probably false positives.
     *
     * @param image     the object containing the image mask
     * @param width     the width of the image
     * @param height    the height of the image
     * @param threshold the number of nearby pixels needed to consider a pixel a valid cloudy pixel
     * @return the number of the removed cloud pixels.
     */
    public static int cleanupCloudsBasedOnNearby(final BufferedImage image, final int width, final int height, final int threshold) {
        int removedPixels = 0;
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (image.getRGB(x, y) == WHITE_RGB) {
                    if (!isAnyNearby(image, x, y, WHITE_RGB, threshold)) {
                        image.setRGB(x, y, BLACK_RGB);
                        removedPixels++;
                    }
                }
            }
        }
        return removedPixels;
    }
    
    
    /**
     * Checks if the nearby pixels of the defined x,y pixel are cloudy or not and the number of the cloud pixels with regard to a given threshold.
     *
     * @param image         the object containing the image mask
     * @param x             the x coordinate of the pixel to check
     * @param y             the y coordinate of the pixel to check
     * @param specificColor the color to check for
     * @param threshold     the number of nearby pixels needed to consider a pixel a valid cloudy pixel
     * @return true if the pixel is considered valid, false if it was a false positive.
     */
    private static boolean isAnyNearby(final BufferedImage image, final int x, final int y, final int specificColor, final int threshold) {
        int nearby = 0;
        nearby += new Color(image.getRGB(x - 1, y)).getRed() > 30 ? 1 : 0;
        nearby += new Color(image.getRGB(x + 1, y)).getRed() > 30 ? 1 : 0;
        nearby += new Color(image.getRGB(x, y - 1)).getRed() > 30 ? 1 : 0;
        nearby += new Color(image.getRGB(x, y + 1)).getRed() > 30 ? 1 : 0;
        nearby += new Color(image.getRGB(x - 1, y - 1)).getRed() > 30 ? 1 : 0;
        nearby += new Color(image.getRGB(x - 1, y + 1)).getRed() > 30 ? 1 : 0;
        nearby += new Color(image.getRGB(x + 1, y - 1)).getRed() > 30 ? 1 : 0;
        nearby += new Color(image.getRGB(x + 1, y + 1)).getRed() > 30 ? 1 : 0;
        return nearby > threshold;
    }
    
}
