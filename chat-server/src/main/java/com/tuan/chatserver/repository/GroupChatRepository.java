package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.GroupChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat,Long> {
    List<GroupChat> findByUsers_Id(Long userId);
    List<GroupChat> findByNameContaining(String name);
}
