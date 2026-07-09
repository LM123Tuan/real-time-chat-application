package com.tuan.chatserver.dto;

import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.MessageStatus;

import java.time.LocalDateTime;

public class MessageDTO {
    private String id;
    private User sender;
    private ChatBox chatBox;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private String content;

    public MessageDTO() {}
    public MessageDTO(String id, User sender, ChatBox chatBox, LocalDateTime timestamp, MessageStatus status, String content) {
        this.id= id;
        this.sender = sender;
        this.chatBox = chatBox;
        this.timestamp = timestamp;
        this.status = status;
        this.content = content;
    }

    public String getId() {
        return id;
    }
    public User getSender() {
        return sender;
    }
    public ChatBox getChatBox() {
        return chatBox;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public MessageStatus getStatus() {
        return status;
    }
    public String getContent() {
        return content;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }
    public void setChatBox(ChatBox chatBox) {
        this.chatBox = chatBox;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    public void setContent(String content) {
        this.content = content;
    }
}
