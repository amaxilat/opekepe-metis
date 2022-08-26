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
     *
     * @param red   the pixel's red value
     * @param green the pixel's green value
     * @param blue  the pixel's blue value
     * @return the calculated brightness value
     */
    public static double getBrightness(final int red, final int green, final int blue) {
        return 0.299 * red + 0.587 * green + 0.114 * blue;
    }
    
    public static double sRGBtoLin(final double colorChannel) {
        // Send this function a decimal sRGB gamma encoded color value
        // between 0.0 and 1.0, and it returns a linearized value.
        if (colorChannel <= 0.04045) {
            return colorChannel / 12.92;
        } else {
            return Math.pow(((colorChannel + 0.055) / 1.055), 2.4);
        }
    }
    
}
