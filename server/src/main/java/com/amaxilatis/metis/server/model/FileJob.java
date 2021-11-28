package com.amaxilatis.metis.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileJob implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    List<Integer> tasks;
}
