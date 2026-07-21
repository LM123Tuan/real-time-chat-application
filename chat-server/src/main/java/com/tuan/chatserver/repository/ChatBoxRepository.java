package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.ChatBox;
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
    @Query("SELECT DISTINCT cb FROM ChatBox cb " +
            "JOIN cb.users filterUser "+
            "JOIN FETCH cb.users " +
            "WHERE filterUser.id = :userId ORDER BY cb.lastActiveTime DESC")
    List<ChatBox> findByUserIdOrderByLastActiveTimeDesc(@Param("userId") Long userId);
    @Query("SELECT DISTINCT cb FROM ChatBox cb " +
            "JOIN cb.users filterUser "+
            "JOIN FETCH cb.users " +
            "WHERE filterUser.id = :userId AND cb.isActive = true ORDER BY cb.lastActiveTime DESC")
    List<ChatBox> findByUserIdAndIsActiveTrueOrderByLastActiveTimeDesc(@Param("userId") Long userId);
    @EntityGraph(attributePaths = {"users"})
    Optional<ChatBox> findById(Long id);
    @EntityGraph(attributePaths = {"users"})
    List<ChatBox> findAll();
    Long countByLastActiveTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}
