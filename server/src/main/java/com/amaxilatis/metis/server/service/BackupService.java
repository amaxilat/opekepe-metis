package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.config.MetisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Statement;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {
    
    private final MetisProperties props;
    private final DataSource dataSource;
    
    @PostConstruct
    public void init() {
        //backup on load
        getBackup();
    }
    
    @Scheduled(cron = "0 0 0 * * ?")
    public void backup() {
        //backup periodically
        getBackup();
    }
    
    public void getBackup() {
        if (StringUtils.isEmpty(props.getDbBackupLocation())) {
            return;
        }
        final String backupLocation = props.getDbBackupLocation() + String.format("\\%d-metisdb-backup\\", System.currentTimeMillis());
        log.info("backing up database to {}", backupLocation);
        try (final Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute(String.format("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE('%s')", backupLocation));
            log.info("stored database backup in {}", backupLocation);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
    
}
