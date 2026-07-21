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
    public DirectMessage(LocalDateTime createTime, Set<User> users, boolean isActive, LocalDateTime lastActiveTime) {
        super(createTime, users, ChatboxType.DIRECT_MESSAGE, isActive, lastActiveTime);
    }
}
