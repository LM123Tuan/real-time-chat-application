package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "direct_message")
public class DirectMessage extends ChatBox{
    public DirectMessage() {}
    public DirectMessage(LocalDateTime createTime, List<User> users, User creator, LocalDateTime lastActiveTime) {
        super(createTime, users, creator, ChatboxType.DIRECT_MESSAGE, lastActiveTime);
    }
}
