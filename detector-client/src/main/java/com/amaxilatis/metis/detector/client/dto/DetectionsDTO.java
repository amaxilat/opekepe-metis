package com.amaxilatis.metis.detector.client.dto;

import lombok.Data;

@Data
public class DetectionsDTO {
    private int w;
    private int h;
    private double[][] predictions;
}
