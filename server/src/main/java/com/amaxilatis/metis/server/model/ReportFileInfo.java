package com.amaxilatis.metis.server.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportFileInfo implements Comparable<ReportFileInfo> {
    private final String name;
    private final String hash;
    private final String date;
    private final String directory;
    private final String path;
    private final double size;
    
    @Override
    public int compareTo(ReportFileInfo o) {
        return name.compareTo(o.getName());
    }
}
