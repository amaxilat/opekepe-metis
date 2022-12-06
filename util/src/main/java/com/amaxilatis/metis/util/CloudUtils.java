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
                                image.setRGB(w * tileSize + i, h * tileSize + j, BLACK_RGB);
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
     * Fills an image mask by turning non-white pixels that are surrounded by more than threshold white pixels.
     *
     * @param image     the object containing the image mask
     * @param threshold the number of nearby pixels needed to turn a black pixel to white.
     * @return the number of pixels turned white
     */
    public static int fillCloudsBasedOnNearby(final BufferedImage image, final int threshold) {
        return fillCloudsBasedOnNearby(image, threshold, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight());
    }
    
    /**
     * Fills an image mask by turning non-white pixels that are surrounded by more than threshold white pixels.
     *
     * @param image       the object containing the image mask
     * @param threshold   the number of nearby pixels needed to turn a black pixel to white.
     * @param startWidth  the x location where the filling should start
     * @param startHeight the y location where the filling should start
     * @param scanWidth   the width to scan pixels
     * @param scanHeight  the height to scan pixels
     * @return the number of pixels turned white
     */
    public static int fillCloudsBasedOnNearby(final BufferedImage image, final int threshold, int startWidth, int startHeight, int scanWidth, int scanHeight) {
        int addedPixels = 0;
        int endX = startWidth + scanWidth - 1;
        if (endX > image.getWidth()) {
            endX = image.getWidth() - 1;
        }
        int endY = startHeight + scanHeight - 1;
        if (endY > image.getHeight()) {
            endY = image.getHeight() - 1;
        }
        for (int x = startWidth + 1; x < endX; x++) {
            for (int y = startHeight + 1; y < endY; y++) {
                if (image.getRGB(x, y) == BLACK_RGB) {
                    if (countNearby(image, x, y, WHITE_RGB) >= threshold) {
                        image.setRGB(x, y, WHITE_RGB);
                        addedPixels++;
                    }
                }
            }
        }
        return addedPixels;
    }
    
    
    /**
     * Cleans up the image mask based on the number of nearby pixels belonging to a cloud, used to remove rogue cloud pixels that are probably false positives.
     *
     * @param image     the object containing the image mask
     * @param threshold the number of nearby pixels needed to consider a pixel a valid cloudy pixel
     * @return the number of the removed cloud pixels.
     */
    public static int cleanupCloudsBasedOnNearby(final BufferedImage image, final int threshold) {
        return cleanupCloudsBasedOnNearby(image, threshold, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight());
    }
    
    /**
     * Cleans up the image mask based on the number of nearby pixels belonging to a cloud, used to remove rogue cloud pixels that are probably false positives.
     *
     * @param image       the object containing the image mask
     * @param startWidth  the x location where the cleanup should start
     * @param startHeight the y location where the cleanup should start
     * @param scanWidth   the width to scan pixels
     * @param scanHeight  the height to scan pixels
     * @param threshold   the number of nearby pixels needed to consider a pixel a valid cloudy pixel
     * @return the number of the removed cloud pixels.
     */
    public static int cleanupCloudsBasedOnNearby(final BufferedImage image, final int threshold, int startWidth, int startHeight, int scanWidth, int scanHeight) {
        int removedPixels = 0;
        int endX = startWidth + scanWidth - 1;
        if (endX > image.getWidth()) {
            endX = image.getWidth() - 1;
        }
        int endY = startHeight + scanHeight - 1;
        if (endY > image.getHeight()) {
            endY = image.getHeight() - 1;
        }
        for (int x = startWidth + 1; x < endX; x++) {
            for (int y = startHeight + 1; y < endY; y++) {
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
     * Cleans up the image mask based on the number consecutive white pixels, used to remove rogue cloud pixels that are probably false positives.
     *
     * @param image       the object containing the image mask
     * @param startWidth  the x location where the cleanup should start
     * @param startHeight the y location where the cleanup should start
     * @param scanWidth   the width to scan pixels
     * @param scanHeight  the height to scan pixels
     * @param threshold   the number of nearby pixels needed to consider a pixel a valid cloudy pixel
     * @return the number of the removed cloud pixels.
     */
    public static int cleanupCloudsBasedOnLines(final BufferedImage image, final int threshold, int startWidth, int startHeight, int scanWidth, int scanHeight) {
        int removedPixels = 0;
        int endX = startWidth + scanWidth;
        if (endX > image.getWidth()) {
            endX = image.getWidth();
        }
        int endY = startHeight + scanHeight;
        if (endY > image.getHeight()) {
            endY = image.getHeight();
        }
        for (int x = startWidth; x < endX; x++) {
            int startY = -1;
            for (int y = startHeight; y < endY; y++) {
                if (image.getRGB(x, y) == WHITE_RGB && (startY == -1)) {
                    startY = y;
                    //                    log.info("startY = {} ", startY);
                }
                if (image.getRGB(x, y) == BLACK_RGB && (startY != -1)) {
                    int length = y - startY;
                    if (length < threshold) {
                        //                        log.info("startY = {} , y = {}", startY, y);
                        for (int y1 = startY; y1 < y; y1++) {
                            image.setRGB(x, y1, BLACK_RGB);
                            removedPixels++;
                        }
                    }
                    startY = -1;
                }
            }
        }
        
        for (int y = startHeight; y < endY; y++) {
            int startX = -1;
            for (int x = startWidth; x < endX; x++) {
                if (image.getRGB(x, y) == WHITE_RGB && (startX == -1)) {
                    startX = x;
                    //                    log.info("startX = {} ", startX);
                }
                if (image.getRGB(x, y) == BLACK_RGB && (startX != -1)) {
                    int length = x - startX;
                    if (length < threshold) {
                        //                        log.info("startX = {} , X = {}", startX, x);
                        for (int x1 = startX; x1 < x; x1++) {
                            image.setRGB(x1, y, GRAY_RGB);
                            removedPixels++;
                        }
                    }
                    startX = -1;
                }
            }
        }
        return removedPixels;
    }
    
    /**
     * Checks and removes white pixels from tiles of the image based on the total number of white-pixels.
     *
     * @param image     the object containing the image mask
     * @param width     the width of the image
     * @param height    the height of the image
     * @param threshold the percentage of white pixels needed to not remove all white pixels from the tile
     * @return the updated image
     */
    public static BufferedImage cleanupCloudsBasedOnThreshold(final BufferedImage image, final int width, final int height, final double threshold) {
        BufferedImage ni = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int sRGBThreshold = (int) (threshold * 256);
        int sRGBThreshold1 = (int) ((1 - threshold) * 256);
        log.info(String.valueOf(sRGBThreshold));
        int removedPixels = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(image.getRGB(x, y));
                int r = c.getRed();
                if (r < sRGBThreshold) {
                    ni.setRGB(x, y, BLACK_RGB);
                    removedPixels++;
                } else if (r > sRGBThreshold1) {
                    ni.setRGB(x, y, WHITE_RGB);
                } else {
                    ni.setRGB(x, y, c.getRGB());
                }
            }
        }
        return ni;
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
        return countNearby(image, x, y, specificColor) > threshold;
    }
    
    /**
     * Counts the number of nearby pixels with the provided color.
     *
     * @param image         the object containing the image mask
     * @param x             the x coordinate of the pixel to check
     * @param y             the y coordinate of the pixel to check
     * @param specificColor the color to check for
     * @return the number of pixels with the provided color.
     */
    private static int countNearby(final BufferedImage image, final int x, final int y, final int specificColor) {
        int nearby = 0;
        nearby += new Color(image.getRGB(x - 1, y)).getRGB() == specificColor ? 1 : 0;
        nearby += new Color(image.getRGB(x + 1, y)).getRGB() == specificColor ? 1 : 0;
        nearby += new Color(image.getRGB(x, y - 1)).getRGB() == specificColor ? 1 : 0;
        nearby += new Color(image.getRGB(x, y + 1)).getRGB() == specificColor ? 1 : 0;
        nearby += new Color(image.getRGB(x - 1, y - 1)).getRGB() == specificColor ? 1 : 0;
        nearby += new Color(image.getRGB(x - 1, y + 1)).getRGB() == specificColor ? 1 : 0;
        nearby += new Color(image.getRGB(x + 1, y - 1)).getRGB() == specificColor ? 1 : 0;
        nearby += new Color(image.getRGB(x + 1, y + 1)).getRGB() == specificColor ? 1 : 0;
        return nearby;
    }
    
    /**
     * Perform a set of cleanup operations in the provided image.
     *
     * @param image       the object containing the image mask
     * @param startWidth  the x location where the cleanup should start
     * @param startHeight the y location where the cleanup should start
     * @param scanWidth   the width to scan pixels
     * @param scanHeight  the height to scan pixels
     * @return the number of white pixels removed from the mask image.
     */
    public static int cleanupNDWI(final BufferedImage image, int startWidth, int startHeight, int scanWidth, int scanHeight) {
        int totalPixelsRemoved = 0;
        int pixelsRemoved = cleanupCloudsBasedOnNearby(image, 2, startWidth, startHeight, scanWidth, scanHeight);
        totalPixelsRemoved += pixelsRemoved;
        
        int totalPixelsAdded = 0;
        for (int i = 0; i < 20; i++) {
            int pixelsAdded = fillCloudsBasedOnNearby(image, 6, startWidth, startHeight, scanWidth, scanHeight);
            totalPixelsAdded += pixelsAdded;
            if (pixelsAdded < 100) {
                break;
            }
        }
        for (int i = 0; i < 20; i++) {
            pixelsRemoved = cleanupCloudsBasedOnLines(image, 20, startWidth, startHeight, scanWidth, scanHeight);
            totalPixelsRemoved += pixelsRemoved;
            if (pixelsRemoved < 500) {
                break;
            }
        }
        
        // cleanup non-white and non-black pixels
        for (int w = startWidth; w < scanWidth; w++) {
            for (int h = startHeight; h < scanHeight; h++) {
                if (image.getRGB(w, h) != WHITE_RGB) {
                    image.setRGB(w, h, BLACK_RGB);
                }
            }
        }
        
        return totalPixelsRemoved - totalPixelsAdded;
    }
    
}
