package com.amaxilatis.metis.server.db.repository;

import com.amaxilatis.metis.server.db.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("Select u from User u WHERE u.username=:username")
    User findByUsername(@Param("username") String username);
    
    @Query("Select u from User u WHERE u.name=:name")
    User findByName(@Param("name") String name);
}
