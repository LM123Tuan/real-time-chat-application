package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "direct_message")
public class DirectMessage extends ChatBox{
    public DirectMessage() {}
    public DirectMessage(LocalDateTime createTime, User creator) {
        super(createTime, creator, ChatboxType.DIRECT_MESSAGE);
    }
}
