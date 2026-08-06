package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "direct_message")
public class DirectMessage extends ChatBox{
    public DirectMessage() {}
    public DirectMessage(String name, LocalDateTime createTime, Set<User> users, boolean isActive, LocalDateTime lastActiveTime) {
        super(name, createTime, users, ChatboxType.DIRECT_MESSAGE, isActive, lastActiveTime);
    }
}
