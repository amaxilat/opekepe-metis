package com.amaxilatis.metis.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoolInfo {
    private int max;
    private int size;
    private int active;
    private long pending;
}
