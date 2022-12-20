package com.amaxilatis.metis.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;

@Slf4j
public class ColorUtils {
    
    public enum LAYERS {
        RED, GREEN, BLUE, NIR, LUM, COLORS
    }
    
    /**
     * Get the brightness value of a pixel
     *
     * @param color the color of the pixel
     * @return the calculated brightness value
     */
    public static double getBrightness(final Color color) {
        return getBrightness(color.getRed(), color.getGreen(), color.getBlue());
    }
    
    /**
     * Get the brightness value of a pixel
     *
     * @param red   the pixel's red value
     * @param green the pixel's green value
     * @param blue  the pixel's blue value
     * @return the calculated brightness value
     */
    public static double getBrightness(final double red, final double green, final double blue) {
        return getBrightness((int) red, (int) green, (int) blue);
    }
    
    /**
     * Get the brightness value of a pixel, using the following coefficients: 0.299 * red + 0.587 * green + 0.114 * blue
     * Value is in accordance with <a href="https://en.wikipedia.org/wiki/Relative_luminance">Relative_luminance - Wikipedia</a>
     * Gimp uses 0.22 * red + 0.72 * green + 0.06 * blue
     * Photoshop uses almost the same weights as ours
     *
     * @param red   the pixel's red value
     * @param green the pixel's green value
     * @param blue  the pixel's blue value
     * @return the calculated brightness value
     */
    public static double getBrightness(final int red, final int green, final int blue) {
        return 0.299 * red + 0.587 * green + 0.114 * blue;
    }
    
    /**
     * Calculates the NDVI index for the given nir and red bands.
     *
     * @param nir the nir band value
     * @param red the red band value
     * @return the calculated NDVI index.
     */
    public static double getNDVI(final double nir, final double red) {
        return ((((nir - red) / (nir + red)) + 1) / 2);
    }
    
    /**
     * Calculates the BSI index for the given nir, red and blue bands.
     *
     * @param nir  the nir band value
     * @param red  the red band value
     * @param blue the blue band value
     * @return the calculated BSI index value.
     */
    public static double getBSI(final double nir, final double red, final double blue) {
        return (((red - (blue + nir)) / (red + blue + nir)) + 1) / 2;
    }
    
    /**
     * Calculates the NDWI index for the given nir and green bands.
     *
     * @param nir   the nir band value
     * @param green the green band value
     * @return the calculated NDWI index value
     * @throws IllegalArgumentException
     * @throws ArithmeticException
     */
    public static double getNDWI(final double nir, final double green) throws IllegalArgumentException, ArithmeticException {
        return ((((green - nir) / (green + nir)) + 1) / 2);
    }
    
    /**
     * Checks if the color is very dark.
     *
     * @param rgb the rgb color
     * @return true if the color is dark false otherwise.
     */
    public static boolean isDark(final int rgb) {
        final Color c = new Color(rgb);
        return isDark(c.getRed(), c.getGreen(), c.getBlue());
    }
    
    /**
     * Checks if the color is very dark.
     *
     * @param r the red band of the color
     * @param g the green band of the color
     * @param b the blue band of the color
     * @return true if the color is dark false otherwise.
     */
    public static boolean isDark(final int r, final int g, final int b) {
        return r < 10 && g < 10 && b < 10;
    }
    
}
