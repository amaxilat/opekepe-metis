package com.amaxilatis.metis.server.db.repository;

import com.amaxilatis.metis.server.db.model.Report;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends DataTablesRepository<Report, Long> {
    
    @Query(value = "CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(:backupName)", nativeQuery = true)
    void backup(@Param("backupName") final String backupName);
    
}
