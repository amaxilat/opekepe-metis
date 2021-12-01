package com.amaxilatis.metis.util.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileJobResult implements Serializable, Comparable<FileJobResult> {
    private static final long serialVersionUID = 1L;
    String name;
    Integer task;
    Boolean result;
    String note;
    
    @Override
    public int compareTo(final FileJobResult o) {
        return getTask() - o.getTask();
    }
}
