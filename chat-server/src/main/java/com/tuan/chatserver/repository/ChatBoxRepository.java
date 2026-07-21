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
            "JOIN FETCH cb.creator " +
            "WHERE filterUser.id = :userId ORDER BY cb.lastActiveTime DESC")
    List<ChatBox> findByUsers_IdOrderByLastActiveTimeDesc(@Param("userId") Long userId);
    @Query("SELECT DISTINCT cb FROM ChatBox cb " +
            "JOIN cb.users filterUser "+
            "JOIN FETCH cb.users " +
            "JOIN FETCH cb.creator " +
            "WHERE filterUser.id = :userId AND cb.isActive = true ORDER BY cb.lastActiveTime DESC")
    List<ChatBox> findByUsers_IdAndIsActiveTrueOrderByLastActiveTimeDesc(@Param("userId") Long userId);
    @EntityGraph(attributePaths = {"users", "creator"})
    Optional<ChatBox> findById(Long id);
    @EntityGraph(attributePaths = {"users", "creator"})
    List<ChatBox> findAll();
    Long countByLastActiveTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}
