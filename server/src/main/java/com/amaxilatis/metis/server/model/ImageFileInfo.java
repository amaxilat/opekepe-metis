package com.amaxilatis.metis.server.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageFileInfo implements Comparable<ImageFileInfo> {
    private final String name;
    private final String hash;
    private final long count;
    
    @Override
    public int compareTo(ImageFileInfo o) {
        return name.compareTo(o.getName());
    }
}
