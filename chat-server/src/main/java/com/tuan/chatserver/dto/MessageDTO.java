package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.MessageStatus;

import java.time.LocalDateTime;

public class MessageDTO {
    private String id;
    private UserDTO sender;
    private ChatBoxDTO chatBox;
    private LocalDateTime timestamp;
    private boolean viewable;
    private MessageStatus status;
    private String content;

    public MessageDTO() {}
    public MessageDTO(String id, UserDTO sender, ChatBoxDTO chatBox, LocalDateTime timestamp, boolean viewable, MessageStatus status, String content) {
        this.id= id;
        this.sender = sender;
        this.chatBox = chatBox;
        this.timestamp = timestamp;
        this.viewable = viewable;
        this.status = status;
        this.content = content;
    }

    public String getId() {
        return id;
    }
    public UserDTO getSender() {
        return sender;
    }
    public ChatBoxDTO getChatBox() {
        return chatBox;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public boolean isViewable() {
        return viewable;
    }
    public MessageStatus getStatus() {
        return status;
    }
    public String getContent() {
        return content;
    }

    public void setSender(UserDTO sender) {
        this.sender = sender;
    }
    public void setChatBox(ChatBoxDTO chatBox) {
        this.chatBox = chatBox;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    public void setViewable(boolean viewable) {
        this.viewable = viewable;
    }
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    public void setContent(String content) {
        this.content = content;
    }
}
