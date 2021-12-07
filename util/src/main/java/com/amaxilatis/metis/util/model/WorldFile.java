package com.amaxilatis.metis.util.model;

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
public class WorldFile implements Serializable {
    private static final long serialVersionUID = 1L;
    double xPixelSize;
    double yRotation;
    double xRotation;
    double yPixelSize;
    double xCenter;
    double yCenter;
}
