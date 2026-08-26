package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage,Long> {
    @Query("SELECT DISTINCT dm FROM DirectMessage dm JOIN dm.users u1 JOIN dm.users u2 " +
            "JOIN FETCH dm.users "+
            "WHERE (u1.id = :userId1 AND u2.id = :userId2) " +
            "OR (u1.id = :userId2 AND u2.id = :userId1)")
    Optional<DirectMessage> findBetweenTwoUsers(@Param("userId1") Long userId1,@Param("userId2") Long userId2);

    @Query("SELECT COUNT(dm) > 0 FROM DirectMessage dm JOIN dm.users u1 JOIN dm.users u2 " +
            "WHERE (u1.id = :userId1 AND u2.id = :userId2) " +
            "OR (u1.id = :userId2 AND u2.id = :userId1)")
    boolean existsBetweenTwoUsers(@Param("userId1") Long userId1,@Param("userId2") Long userId2);

    @Query("SELECT dm FROM DirectMessage dm "+
            "JOIN dm.users u "+
            "WHERE u.id = :userId "+
            "ORDER BY dm.lastActiveTime DESC, dm.id DESC")
    List<DirectMessage> findByUserIdOfFirstPage(@Param("userId") Long userId,
                                                Pageable pageable);

    @Query("SELECT dm FROM DirectMessage dm "+
            "JOIN dm.users u "+
            "WHERE u.id = :userId "+
            "AND (dm.lastActiveTime < :timestamp OR (dm.lastActiveTime = :timestamp AND dm.id < :cursorId)) "+
            "ORDER BY dm.lastActiveTime DESC, dm.id DESC")
    List<DirectMessage> findByUserIdOfNextPage(@Param("userId") Long userId,
                                               @Param("timestamp") LocalDateTime timestamp,
                                               @Param("cursorId") Long cursorId,
                                               Pageable pageable);

    @Query("SELECT DISTINCT dm FROM DirectMessage dm "+
            "JOIN FETCH dm.users "+
            "WHERE dm.id IN :ids")
    List<DirectMessage> findByIdInWithUsers(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"users"})
    Optional<DirectMessage> findById(Long id);
}
