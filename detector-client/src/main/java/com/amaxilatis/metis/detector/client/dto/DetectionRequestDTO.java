package com.amaxilatis.metis.detector.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectionRequestDTO {
    private String image;
    private String mask;
}
