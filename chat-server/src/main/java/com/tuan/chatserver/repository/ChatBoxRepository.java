package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.ChatBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatBoxRepository extends JpaRepository<ChatBox,Long> {
    List<ChatBox> findByUsers_Id(Long userId);
}
