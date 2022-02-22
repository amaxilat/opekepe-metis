package com.amaxilatis.metis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistogramBin {
    private final int binIndex;
    private final long valuesCount;
}
