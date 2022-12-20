package com.amaxilatis.metis.server.db.model;

import com.amaxilatis.metis.model.TestConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Configuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private Date date;
    private String username;
    private double n1PixelSize;
    private int n2BitSize;
    private int n3SamplesPerPixel;
    private double n4CloudCoverageThreshold;
    private double n5ClippingThreshold;
    private double n7VariationLow;
    private double n7VariationHigh;
    private double n9ColorBalanceThreshold;
    private double n9NoiseThreshold;
    
    public TestConfiguration toTestConfiguration(final boolean storeMasks) {
        return new TestConfiguration(n1PixelSize, n2BitSize, n3SamplesPerPixel, n4CloudCoverageThreshold, n5ClippingThreshold, n7VariationLow, n7VariationHigh, n9ColorBalanceThreshold, n9NoiseThreshold, storeMasks);
    }
    
    public TestConfiguration toTestConfiguration() {
        return this.toTestConfiguration(true);
    }
}
