package com.amaxilatis.metis.model;


import com.amaxilatis.metis.util.ColorUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.amaxilatis.metis.util.ColorUtils.LAYERS.BLUE;
import static com.amaxilatis.metis.util.ColorUtils.LAYERS.COLORS;
import static com.amaxilatis.metis.util.ColorUtils.LAYERS.GREEN;
import static com.amaxilatis.metis.util.ColorUtils.LAYERS.LUM;
import static com.amaxilatis.metis.util.ColorUtils.LAYERS.NIR;
import static com.amaxilatis.metis.util.ColorUtils.LAYERS.RED;

@Slf4j
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistogramsHelper {
    
    
    private final Map<ColorUtils.LAYERS, Histogram> bins;
    private final Map<ColorUtils.LAYERS, SummaryStatistics> stats;
    
    public HistogramsHelper() {
        this(256);
    }
    
    public HistogramsHelper(final int binCount) {
        this.bins = new HashMap<>();
        this.bins.put(RED, new Histogram(binCount));
        this.bins.put(GREEN, new Histogram(binCount));
        this.bins.put(BLUE, new Histogram(binCount));
        this.bins.put(NIR, new Histogram(binCount));
        this.bins.put(LUM, new Histogram(binCount));
        this.bins.put(COLORS, new Histogram(binCount));
        
        this.stats = new HashMap<>();
        this.stats.put(RED, new SummaryStatistics());
        this.stats.put(GREEN, new SummaryStatistics());
        this.stats.put(BLUE, new SummaryStatistics());
        this.stats.put(NIR, new SummaryStatistics());
        this.stats.put(LUM, new SummaryStatistics());
        this.stats.put(COLORS, new SummaryStatistics());
    }
    
    /**
     * Add a pixel value to the histograms for all colors.
     *
     * @param red   the red pixel value
     * @param green the green pixel value
     * @param blue  the blue pixel value
     * @param nir   the nir pixel value
     */
    public void addValues(final double red, final double green, final double blue, final double nir) {
        final double brightness = ColorUtils.getBrightness(red, green, blue);
        
        stats.get(LUM).addValue(brightness);
        
        stats.get(COLORS).addValue(red);
        stats.get(COLORS).addValue(green);
        stats.get(COLORS).addValue(blue);
        stats.get(RED).addValue(red);
        stats.get(GREEN).addValue(green);
        stats.get(BLUE).addValue(blue);
        stats.get(NIR).addValue(nir);
        
        addToBin(bins.get(LUM), brightness);
        addToBin(bins.get(COLORS), red);
        addToBin(bins.get(COLORS), green);
        addToBin(bins.get(COLORS), blue);
        addToBin(bins.get(RED), red);
        addToBin(bins.get(GREEN), green);
        addToBin(bins.get(BLUE), blue);
        addToBin(bins.get(NIR), nir);
        
    }
    
    /**
     * Add a value to a bin for the histograms
     *
     * @param histogram the histogram
     * @param value     the value to add
     */
    private void addToBin(final Histogram histogram, final double value) {
        histogram.add(value);
    }
    
    /**
     * Get the Major bin for the given histogram
     *
     * @param layer the layer to search for its major bin
     * @return the id of the major bin in the histogram
     */
    public int majorBin(final ColorUtils.LAYERS layer) {
        return bins.get(layer).getMajorBin().getBinIndex();
    }
    
    /**
     * Gets the list of the 5 bins with the lowest indexes
     *
     * @param layer the layer to search for its bottom 5 bins
     * @return the bottom 5 bins and their contents
     */
    public Set<HistogramBin> getBottom5Bins(final ColorUtils.LAYERS layer) {
        return bins.get(layer).getBottom5Bins();
    }
    
    /**
     * Gets the list of the 5 bins with the highest indexes
     *
     * @param layer the layer to search for its high 5 bins
     * @return the bottom 5 bins and their contents
     */
    public Set<HistogramBin> getTop5Bins(final ColorUtils.LAYERS layer) {
        return bins.get(layer).getTop5Bins();
    }
    
    
    /**
     * Get the mean value of the given layer
     *
     * @param layer the layer to search for its mean value
     * @return the mean value of the layer
     */
    public double getMean(final ColorUtils.LAYERS layer) {
        return this.stats.get(layer).getMean();
    }
    
    /**
     * Get the standard deviation value of the given layer
     *
     * @param layer the layer to search for its standard deviation value
     * @return the mean value of the layer
     */
    public double getStandardDeviation(final ColorUtils.LAYERS layer) {
        return this.stats.get(layer).getStandardDeviation();
    }
    
    /**
     * Get the total amount of pixels for the given layer
     *
     * @param layer the layer to search for its total pixels
     * @return the number of pixels in the layer
     */
    public long getTotalPixels(final ColorUtils.LAYERS layer) {
        return this.stats.get(layer).getN();
    }
    
    public void saveHistogramImage(final File destFile) {
        final JFreeChart xylineChart = ChartFactory.createXYLineChart(null, "Τιμή Pixel", "Σύνολο τιμών", createDataset(), PlotOrientation.VERTICAL, true, true, false);
        xylineChart.getXYPlot().getRangeAxis().setVisible(false);
        xylineChart.getXYPlot().getRenderer().setSeriesPaint(3, new Color(0x00, 0x00, 0x00));
        xylineChart.getXYPlot().getRenderer().setSeriesStroke(3, new BasicStroke(2));
        try {
            ChartUtils.saveChartAsPNG(destFile, xylineChart, 1024, 768);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    private XYDataset createDataset() {
        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(createSeries(RED, "R"));
        dataset.addSeries(createSeries(BLUE, "B"));
        dataset.addSeries(createSeries(GREEN, "G"));
        dataset.addSeries(createSeries(LUM, "LUM"));
        return dataset;
    }
    
    private XYSeries createSeries(final ColorUtils.LAYERS layer, final String title) {
        final XYSeries series = new XYSeries(title);
        for (int i = 0; i < bins.get(layer).getData().length; i++) {
            series.add(i, bins.get(layer).getData()[i]);
        }
        return series;
    }
}
