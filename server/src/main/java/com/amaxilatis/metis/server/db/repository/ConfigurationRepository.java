package com.amaxilatis.metis.server.db.repository;

import com.amaxilatis.metis.server.db.model.Configuration;
import com.amaxilatis.metis.server.db.model.Report;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConfigurationRepository extends CrudRepository<Configuration, Long> {
    
}
