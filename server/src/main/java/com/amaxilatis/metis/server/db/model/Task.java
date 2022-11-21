package com.amaxilatis.metis.server.db.model;

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
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private Date date;
    private String outFileName;
    private String fileName;
    private String tasks;
    private Long reportId;
    
    public String reportName() {
        if (outFileName != null) {
            final String[] parts = outFileName.split("/");
            return parts[parts.length - 1];
        } else {
            return "";
        }
    }
    
    public String inFileName() {
        if (fileName != null) {
            final String[] parts = fileName.split("\\\\");
            return parts[parts.length - 1];
        } else {
            return "";
        }
    }
    
    public String inFileDir() {
        if (fileName != null) {
            final String[] parts = fileName.split("\\\\");
            return parts[parts.length - 2];
        } else {
            return "";
        }
    }
}
