package com.amaxilatis.metis.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TestConfiguration {
    private double n1PixelSize;
    private int n2BitSize;
    private int n3SamplesPerPixel;
    private double n4CloudCoverageThreshold;
    private double n5ClippingThreshold;
    private double n7VariationLow;
    private double n7VariationHigh;
    private double n9ColorBalanceThreshold;
    private double n9NoiseThreshold;
}
