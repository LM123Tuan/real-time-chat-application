package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "direct_message")
public class DirectMessage extends ChatBox{
    @Column(name = "conversation_key", unique = true, nullable = false)
    private String conversationKey;

    public DirectMessage() {}
    public DirectMessage(String name, LocalDateTime createTime, Set<User> users, boolean isActive, LocalDateTime lastActiveTime, String conversationKey) {
        super(name, createTime, users, ChatboxType.DIRECT_MESSAGE, isActive, lastActiveTime);
        this.conversationKey=conversationKey;
    }

    public String getConversationKey(){
        return this.conversationKey;
    }

    public void setConversationKey(){
        this.conversationKey=conversationKey;
    }
}
