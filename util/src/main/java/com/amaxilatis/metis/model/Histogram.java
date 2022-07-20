package com.amaxilatis.metis.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Slf4j
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Histogram {
    
    private final long[] data;
    
    public Histogram(int size) {
        this.data = new long[256];
        Arrays.fill(data, 0);
    }
    
    public void add(double value) {
        this.data[(int) value] += 1L;
    }
    
    private SortedSet<HistogramBin> getSortedBins(final Comparator<HistogramBin> comparator) {
        final SortedSet<HistogramBin> bins = new TreeSet<>(comparator);
        for (int i = 1; i < data.length; i++) {
            bins.add(new HistogramBin(i, data[i]));
        }
        return bins;
    }
    
    private SortedSet<HistogramBin> getBinsByDescendingContentsSize() {
        return getSortedBins((o1, o2) -> (int) (o2.getValuesCount() - o1.getValuesCount()));
    }
    
    private SortedSet<HistogramBin> getBinsByAscendingContentsSize() {
        return getSortedBins((o1, o2) -> (int) (o1.getValuesCount() - o2.getValuesCount()));
    }
    
    public HistogramBin getMajorBin() {
        return getBinsByDescendingContentsSize().first();
    }
    
    public Set<HistogramBin> getTop5Bins() {
        final SortedSet<HistogramBin> bins = getBinsByDescendingContentsSize();
        final Set<HistogramBin> topBins = new HashSet<>();
        Iterator<HistogramBin> it = bins.iterator();
        for (int i = 0; i < 5; i++) {
            topBins.add(it.next());
        }
        return topBins;
    }
    
    public Set<HistogramBin> getFirstBins(int count) {
        final SortedSet<HistogramBin> firstBins = new TreeSet<>((o1, o2) -> (int) (o2.getValuesCount() - o1.getValuesCount()));
        for (int i = 0; i < count; i++) {
            firstBins.add(new HistogramBin(i, data[i]));
        }
        return firstBins;
    }
    
    public Set<HistogramBin> getBottom5Bins() {
        final SortedSet<HistogramBin> bins = getBinsByAscendingContentsSize();
        final Set<HistogramBin> botBins = new HashSet<>();
        Iterator<HistogramBin> it = bins.iterator();
        for (int i = 0; i < 5; i++) {
            botBins.add(it.next());
        }
        return botBins;
    }
    
    public Set<HistogramBin> getLastBins(int count) {
        final SortedSet<HistogramBin> lastBins = new TreeSet<>((o1, o2) -> (int) (o1.getValuesCount() - o2.getValuesCount()));
        for (int i = data.length - count; i < data.length; i++) {
            lastBins.add(new HistogramBin(i, data[i]));
        }
        return lastBins;
    }
}
