package com.amaxilatis.metis.server.db.repository;

import com.amaxilatis.metis.server.db.model.Report;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

public interface ReportRepository extends DataTablesRepository<Report, Long> {

}
