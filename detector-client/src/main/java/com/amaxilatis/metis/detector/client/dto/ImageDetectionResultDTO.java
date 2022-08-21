package com.amaxilatis.metis.detector.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageDetectionResultDTO {
    private String image;
    private String mask;
    private int pixels;
    private int cloudy;
    private double percentage;
}
