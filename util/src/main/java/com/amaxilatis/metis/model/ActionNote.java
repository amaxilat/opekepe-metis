package com.amaxilatis.metis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionNote {
    private String dirname;
    private String filename;
    private int test;
    private Date date;
    private boolean start;
    private Boolean result;
    private Long time;
}

