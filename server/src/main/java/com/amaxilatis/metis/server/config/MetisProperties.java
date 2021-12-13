package com.amaxilatis.metis.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "metis")
public class MetisProperties {
    
    private String reportLocation;
    private String filesLocation;
    
}
