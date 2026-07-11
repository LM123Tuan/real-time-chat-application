package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_chat")
public class GroupChat extends ChatBox{
    @Column(name = "name", nullable = false)
    private String name;
    public GroupChat() {}
    public GroupChat(LocalDateTime createTime, User creator, String name) {
        super(createTime, creator, ChatboxType.GROUP_CHAT);
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
