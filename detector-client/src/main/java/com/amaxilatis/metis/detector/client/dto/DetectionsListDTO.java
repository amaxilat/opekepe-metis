package com.amaxilatis.metis.detector.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class DetectionsListDTO {
    private List<DetectionsDTO> tiles;
}
