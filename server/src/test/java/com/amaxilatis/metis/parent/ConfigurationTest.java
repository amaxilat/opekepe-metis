package com.amaxilatis.metis.parent;

import com.amaxilatis.metis.model.TestConfiguration;
import com.amaxilatis.metis.server.db.model.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ConfigurationTest {
    private final static int N2_BIT_SIZE = 8;
    private final static boolean STORE_MASKS = true;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigurationTest.class);
    
    private static Configuration configuration;
    
    @BeforeAll
    static void beforeAll() {
        configuration = new Configuration();
    }
    
    @BeforeEach
    void beforeEach() {
        configuration.setN2BitSize(N2_BIT_SIZE);
    }
    
    @Test
    void testConvert2TestConfiguration() {
        final TestConfiguration tConfiguration = configuration.toTestConfiguration(STORE_MASKS);
        assertThat(tConfiguration.getN2BitSize()).isEqualTo(N2_BIT_SIZE);
        assertThat(tConfiguration.isStoreMasks()).isTrue();
    }
    
}
