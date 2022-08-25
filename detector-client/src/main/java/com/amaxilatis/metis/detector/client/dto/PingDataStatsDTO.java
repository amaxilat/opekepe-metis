package com.amaxilatis.metis.detector.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PingDataStatsDTO {
    private Double load;
    private Double prepare;
    private Double predict;
    private Double reply;
}
