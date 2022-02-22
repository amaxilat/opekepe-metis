package com.amaxilatis.metis.server.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestDescription implements Comparable<TestDescription> {
    private int id;
    private String name;
    private String description;
    private boolean enabled;
    
    @Override
    public int compareTo(TestDescription o) {
        return this.id - o.getId();
    }
}
