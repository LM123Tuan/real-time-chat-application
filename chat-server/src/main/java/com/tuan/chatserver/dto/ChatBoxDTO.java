package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;

public class ChatBoxDTO {
    private Long id;
    private String name;
    private ChatboxType chatboxType;
    private boolean isActive;
    private LocalDateTime lastActiveTime;

    public ChatBoxDTO() {}
    public ChatBoxDTO(Long id, String name, ChatboxType chatboxType, boolean isActive, LocalDateTime lastActiveTime) {
        this.id = id;
        this.name=name;
        this.chatboxType = chatboxType;
        this.isActive = isActive;
        this.lastActiveTime = lastActiveTime;
    }

    public Long getId(){
        return id;
    }
    public String getName() {return name;}
    public ChatboxType getChatboxType(){
        return chatboxType;
    }
    public boolean isActive(){
        return isActive;
    }
    public LocalDateTime getLastActiveTime(){
        return lastActiveTime;
    }

    public void setName(String name){this.name=name;}
    public void setChatboxType(ChatboxType chatboxType){
        this.chatboxType = chatboxType;
    }
    public void setActive(boolean isActive){
        this.isActive = isActive;
    }
    public void setLastActiveTime(LocalDateTime lastActiveTime){
        this.lastActiveTime = lastActiveTime;
    }
}
