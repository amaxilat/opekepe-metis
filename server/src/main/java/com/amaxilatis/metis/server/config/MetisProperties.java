package com.amaxilatis.metis.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "metis")
public class MetisProperties {
    
    private String reportLocation;
    private String filesLocation;
    private String thumbnailLocation;
    private String histogramLocation;
    private String cloudMaskLocation;
    private String uncompressedLocation;
    private String resultsLocation;
    private String dbBackupLocation;
    private String version;
    
}
