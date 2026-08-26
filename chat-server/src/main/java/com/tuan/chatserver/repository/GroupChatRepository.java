package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.GroupChat;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat,Long> {

    @Query("SELECT gc FROM GroupChat gc " +
            "JOIN gc.users filterUser " +
            "WHERE filterUser.id = :userId " +
            "AND gc.name LIKE CONCAT('%', :name, '%') " +
            "AND gc.isActive = true " +
            "ORDER BY gc.lastActiveTime DESC, gc.id DESC")
    List<GroupChat> findByNameContainingAndUserIdOfFirstPage(@Param("name") String name,
                                                             @Param("userId") Long userId,
                                                             Pageable pageable);

    @Query("SELECT gc FROM GroupChat gc " +
            "JOIN gc.users filterUser " +
            "WHERE filterUser.id = :userId " +
            "AND gc.name LIKE CONCAT('%', :name, '%') " +
            "AND gc.isActive = true " +
            "AND (gc.lastActiveTime < :timestamp " +
            "     OR (gc.lastActiveTime = :timestamp AND gc.id < :cursorId)) " +
            "ORDER BY gc.lastActiveTime DESC, gc.id DESC")
    List<GroupChat> findByNameContainingAndUserIdOfNextPage(@Param("name") String name,
                                                            @Param("userId") Long userId,
                                                            @Param("timestamp") LocalDateTime timestamp,
                                                            @Param("cursorId") Long cursorId,
                                                            Pageable pageable);

    @Query("SELECT DISTINCT gc FROM GroupChat gc "+
            " JOIN FETCH gc.users "+
            " JOIN FETCH gc.leaders "+
            " JOIN FETCH gc.viceLeaders "+
            " WHERE gc.id = :id ")
    Optional<GroupChat> findById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000")})
    @Query("SELECT DISTINCT gc FROM GroupChat gc "+
            " JOIN FETCH gc.users "+
            " JOIN FETCH gc.leaders "+
            " JOIN FETCH gc.viceLeaders "+
            " WHERE gc.id = :id ")
    Optional<GroupChat> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT gc FROM GroupChat gc " +
            "JOIN gc.users filterUser " +
            "WHERE filterUser.id = :userId AND gc.isActive = true " +
            "ORDER BY gc.lastActiveTime DESC, gc.id DESC")
    List<GroupChat> findByUserIdOfFirstPage(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT gc FROM GroupChat gc " +
            "JOIN gc.users filterUser " +
            "WHERE filterUser.id = :userId AND gc.isActive = true " +
            "AND (gc.lastActiveTime < :timestamp " +
            "     OR (gc.lastActiveTime = :timestamp AND gc.id < :cursorId)) " +
            "ORDER BY gc.lastActiveTime DESC, gc.id DESC")
    List<GroupChat> findByUserIdOfNextPage(@Param("userId") Long userId,
                                           @Param("timestamp") LocalDateTime timestamp,
                                           @Param("cursorId") Long cursorId,
                                           Pageable pageable);

    @Query("SELECT DISTINCT gc FROM GroupChat gc " +
            "JOIN FETCH gc.users " +
            "JOIN FETCH gc.leaders " +
            "JOIN FETCH gc.viceLeaders " +
            "WHERE gc.id IN :ids")
    List<GroupChat> findByIdInWithUsers(@Param("ids") List<Long> ids);
}
