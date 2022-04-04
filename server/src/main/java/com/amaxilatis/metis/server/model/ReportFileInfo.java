package com.amaxilatis.metis.server.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ReportFileInfo implements Comparable<ReportFileInfo> {
    private final long id;
    private final String hash;
    private final Date date;
    private final String directory;
    private final double size;
    
    @Override
    public int compareTo(ReportFileInfo o) {
        return date.compareTo(o.getDate());
    }
    
    public String name() {
        return "metis-" + id;
    }
}
