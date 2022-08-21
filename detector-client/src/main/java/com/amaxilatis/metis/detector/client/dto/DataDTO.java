package com.amaxilatis.metis.detector.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataDTO {
    private int w;
    private int h;
    private int[] data;
}
