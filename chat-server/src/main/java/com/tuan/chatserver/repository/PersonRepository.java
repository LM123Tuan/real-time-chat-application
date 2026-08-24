package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Person p SET p.tokenVersion = p.tokenVersion + 1 WHERE p.id = :id")
    int incrementTokenVersion(@Param("id") Long id);
}
