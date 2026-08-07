package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.ChatBox;
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
public interface ChatBoxRepository extends JpaRepository<ChatBox,Long> {
    @Query("SELECT cb FROM ChatBox cb " +
            "JOIN cb.users filterUser " +
            "WHERE filterUser.id = :userId " +
            "ORDER BY cb.lastActiveTime DESC, cb.id DESC")
    List<ChatBox> findByUserIdOfFirstPage(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT cb FROM ChatBox cb " +
            "JOIN cb.users filterUser " +
            "WHERE filterUser.id = :userId " +
            "AND (cb.lastActiveTime < :timestamp " +
            "     OR (cb.lastActiveTime = :timestamp AND cb.id < :cursorId)) " +
            "ORDER BY cb.lastActiveTime DESC, cb.id DESC")
    List<ChatBox> findByUserIdOfNextPage(@Param("userId") Long userId,
                                         @Param("timestamp") LocalDateTime timestamp,
                                         @Param("cursorId") Long cursorId,
                                         Pageable pageable);

    @Query("SELECT cb FROM ChatBox cb " +
            "JOIN cb.users filterUser " +
            "WHERE filterUser.id = :userId AND cb.isActive = true " +
            "ORDER BY cb.lastActiveTime DESC, cb.id DESC")
    List<ChatBox> findByUserIdAndIsActiveTrueOfFirstPage(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT DISTINCT cb FROM ChatBox cb " +
            "JOIN cb.users filterUser " +
            "WHERE filterUser.id = :userId AND cb.isActive = true " +
            "AND (cb.lastActiveTime < :cursorTimestamp " +
            "     OR (cb.lastActiveTime = :cursorTimestamp AND cb.id < :cursorId)) " +
            "ORDER BY cb.lastActiveTime DESC, cb.id DESC")
    List<ChatBox> findByUserIdAndIsActiveTrueOfNextPage(@Param("userId") Long userId,
                                                        @Param("cursorTimestamp") LocalDateTime cursorTimestamp,
                                                        @Param("cursorId") Long cursorId,
                                                        Pageable pageable);

    @Query("SELECT DISTINCT cb FROM ChatBox cb " +
            "JOIN FETCH cb.users " +
            "WHERE cb.id IN :ids")
    List<ChatBox> findByIdInWithUsers(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"users"})
    Optional<ChatBox> findById(Long id);

    @Query("SELECT cb FROM ChatBox cb ORDER BY cb.id DESC")
    List<ChatBox> findAllOfFirstPage(Pageable pageable);

    @Query("SELECT cb FROM ChatBox cb WHERE cb.id < :cursorId ORDER BY cb.id DESC")
    List<ChatBox> findAllOfNextPage(@Param("cursorId") Long cursorId, Pageable pageable);

    Long countByLastActiveTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}
