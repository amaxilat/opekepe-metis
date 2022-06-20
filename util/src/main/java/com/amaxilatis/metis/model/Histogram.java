package com.amaxilatis.metis.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
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
    
    public HistogramBin getMajorBin() {
        final SortedSet<HistogramBin> bins = new TreeSet<>((o1, o2) -> (int) (o2.getValuesCount() - o1.getValuesCount()));
        for (int i = 1; i < data.length; i++) {
            bins.add(new HistogramBin(i, data[i]));
        }
//        for (HistogramBin bin : bins) {
//            log.info("Bins: " + bin.getBinIndex() + " size " + bin.getValuesCount());
//        }
        return bins.first();
    }
    
    public Set<HistogramBin> getTop5Bins() {
        final SortedSet<HistogramBin> bins = new TreeSet<>((o1, o2) -> (int) (o2.getValuesCount() - o1.getValuesCount()));
        for (int i = 1; i < data.length; i++) {
            bins.add(new HistogramBin(i, data[i]));
        }
        final Set<HistogramBin> topBins = new HashSet<>();
        Iterator<HistogramBin> it = bins.iterator();
        for (int i = 0; i < 5; i++) {
            topBins.add(it.next());
        }
        return topBins;
    }
    
    public Set<HistogramBin> getBottom5Bins() {
        final SortedSet<HistogramBin> bins = new TreeSet<>((o1, o2) -> (int) (o1.getValuesCount() - o2.getValuesCount()));
        for (int i = 1; i < data.length; i++) {
            bins.add(new HistogramBin(i, data[i]));
        }
        final Set<HistogramBin> botBins = new HashSet<>();
        Iterator<HistogramBin> it = bins.iterator();
        for (int i = 0; i < 5; i++) {
            botBins.add(it.next());
        }
        return botBins;
    }
}
