package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.Set;

public class ChatBoxDTO {
    private Long id;
    private LocalDateTime createTime;
    private Set<MyProfileDTO> users;
    private ChatboxType chatboxType;
    private boolean isActive;
    private LocalDateTime lastActiveTime;

    public ChatBoxDTO() {}
    public ChatBoxDTO(Long id, LocalDateTime createTime, Set<MyProfileDTO> users, ChatboxType chatboxType, boolean isActive, LocalDateTime lastActiveTime) {
        this.id = id;
        this.createTime = createTime;
        this.users = users;
        this.chatboxType = chatboxType;
        this.isActive = isActive;
        this.lastActiveTime = lastActiveTime;
    }

    public Long getId(){
        return id;
    }
    public LocalDateTime getCreateTime(){
        return createTime;
    }
    public Set<MyProfileDTO> getUsers(){
        return users;
    }
    public ChatboxType getChatboxType(){
        return chatboxType;
    }
    public boolean isActive(){
        return isActive;
    }
    public LocalDateTime getLastActiveTime(){
        return lastActiveTime;
    }

    public void setCreateTime(LocalDateTime createTime){
        this.createTime = createTime;
    }
    public void setUsers(Set<MyProfileDTO> users){
        this.users = users;
    }
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
