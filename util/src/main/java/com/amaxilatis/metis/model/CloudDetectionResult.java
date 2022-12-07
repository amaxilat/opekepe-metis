package com.amaxilatis.metis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudDetectionResult {
    private String image;
    private String mask;
    private int pixels;
    private int cloudy;
    private double percentage;
}
