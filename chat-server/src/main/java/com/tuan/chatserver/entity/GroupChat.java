package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_chat")
public class GroupChat extends ChatBox{
    public GroupChat() {}
    public GroupChat(LocalDateTime createTime, User creator) {
        super(createTime, creator, ChatboxType.GROUP_CHAT);
    }
}
