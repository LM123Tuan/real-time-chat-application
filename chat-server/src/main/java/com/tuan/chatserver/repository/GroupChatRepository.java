package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.GroupChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat,Long> {
    @Query("SELECT DISTINCT gc FROM GroupChat gc "+
            " JOIN gc.users filterUser "+
            " JOIN FETCH gc.users"+
            " JOIN FETCH gc.leaders "+
            " JOIN FETCH gc.viceLeaders "+
            " WHERE filterUser.id = :userId AND gc.name LIKE CONCAT('%', :name, '%') AND gc.isActive = true ORDER BY gc.lastActiveTime DESC")
    List<GroupChat> findByNameContainingAndUserIdAndIsActiveTrueOrderByLastActiveTimeDesc(@Param("name") String name,@Param("userId") Long userId);
    @Query("SELECT DISTINCT gc FROM GroupChat gc "+
            " JOIN FETCH gc.users "+
            " JOIN FETCH gc.leaders "+
            " JOIN FETCH gc.viceLeaders "+
            " WHERE gc.id = :id ")
    Optional<GroupChat> findById(@Param("id") Long id);
    @Query("SELECT DISTINCT gc FROM GroupChat gc "+
            " JOIN gc.users filterUser "+
            " JOIN FETCH gc.users"+
            " JOIN FETCH gc.leaders "+
            " JOIN FETCH gc.viceLeaders "+
            " WHERE filterUser.id = :userId AND gc.isActive = true")
    List<GroupChat> findByUserIdAndIsActiveTrue(@Param("userId") Long userId);
}
