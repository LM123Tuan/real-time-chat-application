package com.tuan.chatserver.document;

import com.tuan.chatserver.enums.MessageStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "message")
public class Message {
    @Id
    private String id;
    @Field("sender_id")
    @NotNull(message = "Sender ID cannot be empty")
    private Long senderId;
    @Field("chatbox_id")
    @NotNull(message = "ChatBox ID cannot be empty")
    private Long chatBoxId;
    @NotNull(message = "Timestamp cannot be empty")
    private LocalDateTime timestamp;
    @Field("status")
    @NotNull(message = "Status cannot be empty")
    private MessageStatus status;
    @NotBlank(message = "Content cannot be empty")
    private String content;

    public Message() {}
    public Message(Long senderId, Long chatBoxId, LocalDateTime timestamp, MessageStatus status, String content) {
        this.senderId = senderId;
        this.chatBoxId = chatBoxId;
        this.timestamp = timestamp;
        this.status = status;
        this.content = content;
    }

    public String getId() {
        return id;
    }
    public Long getSenderId() {
        return senderId;
    }
    public Long getChatBoxId() {
        return chatBoxId;
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

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    public void setChatBoxId(Long chatBoxId) {
        this.chatBoxId = chatBoxId;
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
