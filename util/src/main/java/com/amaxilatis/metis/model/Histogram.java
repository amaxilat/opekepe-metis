package com.amaxilatis.metis.model;


import com.amaxilatis.metis.util.ColorUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.amaxilatis.metis.util.ColorUtils.LAYERS.*;

@Slf4j
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Histogram {
    private final Map<ColorUtils.LAYERS, Map<Integer, Long>> bins;
    private final Map<ColorUtils.LAYERS, SummaryStatistics> stats;
    private final int binSize;
    
    public Histogram() {
        this(1);
    }
    
    public Histogram(final int binSize) {
        this.binSize = binSize;
        this.bins = new HashMap<>();
        this.bins.put(RED, new HashMap<>());
        this.bins.put(GREEN, new HashMap<>());
        this.bins.put(BLUE, new HashMap<>());
        this.bins.put(NIR, new HashMap<>());
        this.bins.put(LUM, new HashMap<>());
        this.bins.put(COLORS, new HashMap<>());
        
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
        final double br = ColorUtils.getBrightness((int) red, (int) green, (int) blue);
        
        stats.get(LUM).addValue(br);
        
        stats.get(COLORS).addValue(red);
        stats.get(COLORS).addValue(green);
        stats.get(COLORS).addValue(blue);
        stats.get(RED).addValue(red);
        stats.get(GREEN).addValue(green);
        stats.get(BLUE).addValue(blue);
        stats.get(NIR).addValue(nir);
        
        addToBin(bins.get(LUM), br);
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
     * @param binMap the histogram's bins
     * @param value  the value to add
     */
    private void addToBin(final Map<Integer, Long> binMap, final double value) {
        int binR = getBin(value, binSize);
        if (!binMap.containsKey(binR)) {
            binMap.put(binR, 1L);
        } else {
            binMap.put(binR, binMap.get(binR) + 1);
        }
    }
    
    /**
     * Get the bin that the provided value belongs to based on the given binSize
     *
     * @param value   the value to assign to a bin
     * @param binSize the size of each bin
     * @return the id of the bin the value belongs to
     */
    private static int getBin(final double value, final int binSize) {
        return (int) ((value / binSize)) * binSize;
    }
    
    /**
     * Get the Major bin for the given histogram
     *
     * @param layer the layer to search for its major bin
     * @return the id of the major bin in the histogram
     */
    public int majorBin(final ColorUtils.LAYERS layer) {
        Integer majorBin = null;
        Long majorBinSize = null;
        for (final Map.Entry<Integer, Long> entry : bins.get(layer).entrySet()) {
            //            log.info("{}:{}", entry.getKey(), entry.getValue());
            if (majorBinSize == null || entry.getValue() > majorBinSize) {
                majorBinSize = entry.getValue();
                majorBin = entry.getKey();
            }
        }
        return majorBin != null ? majorBin : -1;
    }
    
    /**
     * Gets the list of the 5 bins with the lowest indexes
     *
     * @param layer the layer to search for its bottom 5 bins
     * @return the bottom 5 bins and their contents
     */
    public Set<HistogramBin> getBottom5Bins(final ColorUtils.LAYERS layer) {
        final Set<HistogramBin> rbins = new HashSet<>();
        final SortedSet<Integer> s = new TreeSet<>((o1, o2) -> o1 - o2);
        s.addAll(bins.get(layer).keySet());
        final Iterator<Integer> it = s.iterator();
        for (int i = 0; i < 5; i++) {
            Integer binId = it.next();
            rbins.add(new HistogramBin(binId, bins.get(layer).get(binId)));
        }
        return rbins;
    }
    
    /**
     * Gets the list of the 5 bins with the highest indexes
     *
     * @param layer the layer to search for its high 5 bins
     * @return the bottom 5 bins and their contents
     */
    public Set<HistogramBin> getTop5Bins(final ColorUtils.LAYERS layer) {
        final Set<HistogramBin> rbins = new HashSet<>();
        final SortedSet<Integer> s = new TreeSet<>((o1, o2) -> o2 - o1);
        s.addAll(bins.get(layer).keySet());
        final Iterator<Integer> it = s.iterator();
        for (int i = 0; i < 5; i++) {
            Integer binId = it.next();
            rbins.add(new HistogramBin(binId, bins.get(layer).get(binId)));
        }
        return rbins;
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
}
