package com.amaxilatis.metis.server.db.repository;

import com.amaxilatis.metis.server.db.model.Task;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends DataTablesRepository<Task, Long> {
    Optional<Task> findFirstByOrderByIdAsc();
    
    long countByReportId(long reportId);
    
    List<Task> findTasksByReportId(long reportId);
}
