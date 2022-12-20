package com.amaxilatis.metis.server.db.repository;

import com.amaxilatis.metis.server.db.model.Configuration;
import org.springframework.data.repository.CrudRepository;

public interface ConfigurationRepository extends CrudRepository<Configuration, Long> {
    
}
