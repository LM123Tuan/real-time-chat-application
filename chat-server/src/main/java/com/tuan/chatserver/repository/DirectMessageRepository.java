package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.DirectMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage,Long> {
    @EntityGraph(attributePaths = {"users", "creator"})
    @Query("SELECT dm FROM DirectMessage dm JOIN dm.users u1 JOIN dm.users u2 " +
            "WHERE (u1.id = :userId1 AND u2.id = :userId2) " +
            "OR (u1.id = :userId2 AND u2.id = :userId1)")
    Optional<DirectMessage> findBetweenTwoUsers(@Param("userId1") Long userId1,@Param("userId2") Long userId2);
    @Query("SELECT COUNT(dm) > 0 FROM DirectMessage dm JOIN dm.users u1 JOIN dm.users u2 " +
            "WHERE (u1.id = :userId1 AND u2.id = :userId2) " +
            "OR (u1.id = :userId2 AND u2.id = :userId1)")
    boolean existsBetweenTwoUsers(@Param("userId1") Long userId1,@Param("userId2") Long userId2);
    @EntityGraph(attributePaths = {"users","creator"})
    Optional<DirectMessage> findById(Long id);
}
