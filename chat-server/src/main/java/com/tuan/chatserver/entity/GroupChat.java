package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "group_chat")
public class GroupChat extends ChatBox{
    //Can them isActive de xem nhom con ton tai khong (truong hop moi thanh vien deu out het)
    @Column(name = "name", nullable = false)
    private String name;
    public GroupChat() {}
    public GroupChat(LocalDateTime createTime, List<User> users, User creator, String name, LocalDateTime lastActiveTime) {
        super(createTime, users, creator, ChatboxType.GROUP_CHAT, lastActiveTime);
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
