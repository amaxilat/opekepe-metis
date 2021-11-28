package com.amaxilatis.metis.server;

import com.amaxilatis.metis.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class MetisApplication {
    
    
    public static void main(String[] args) {
        log.info(Utils.NAME);
        SpringApplication.run(MetisApplication.class, args);
    }
    
}
