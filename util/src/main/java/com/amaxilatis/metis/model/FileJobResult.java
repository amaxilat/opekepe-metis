package com.amaxilatis.metis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileJobResult implements Serializable, Comparable<FileJobResult> {
    private static final long serialVersionUID = 1L;
    String name;
    Integer task;
    Boolean result;
    String note;
    
    //metrics
    Double n1XPixelSizeWorld;
    Double n1YPixelSizeWorld;
    Double n1XPixelSize;
    Double n1YPixelSize;
    Integer n2BitSize;
    Integer n3SamplesPerPixel;
    Integer n3SamplesPerPixelColor;
    Boolean n3HasAlpha;
    Double n4CloudCoverage;
    Double b5TopClipping;
    Double n5BottomClipping;
    Integer n6LumHistCenter;
    Double n7CoefficientOfVariation;
    String n8Compression;
    Double n9ColorBalance;
    Double n9RedSnr;
    Double n9GreenSnr;
    Double n9BlueSnr;
    
    @Override
    public int compareTo(final FileJobResult o) {
        return getTask() - o.getTask();
    }
}
