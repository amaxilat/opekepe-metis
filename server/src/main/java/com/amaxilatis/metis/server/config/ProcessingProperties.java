package com.amaxilatis.metis.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "metis.processing")
public class ProcessingProperties {
    
    private int threads;
    
}
