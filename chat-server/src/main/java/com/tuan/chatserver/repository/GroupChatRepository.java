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
            "JOIN gc.users filterUser "+
            " JOIN FETCH gc.users"+
            " JOIN FETCH gc.creator "+
            " WHERE filterUser.id = :userId AND gc.name LIKE CONCAT('%', :name, '%') ORDER BY gc.lastActiveTime DESC")
    List<GroupChat> findByNameContainingAndUsers_IdOrderByLastActiveTimeDesc(@Param("name") String name,@Param("userId") Long userId);
    @Query("SELECT DISTINCT gc FROM GroupChat gc "+
            " JOIN FETCH gc.users "+
            " JOIN FETCH gc.creator "+
            " WHERE gc.id = :id ")
    Optional<GroupChat> findById(@Param("id") Long id);
}
