package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatBoxDTO {
    private Long id;
    private LocalDateTime createTime;
    private List<UserDTO> users;
    private UserDTO creator;
    private ChatboxType chatboxType;
    private LocalDateTime lastActiveTime;

    public ChatBoxDTO() {}
    public ChatBoxDTO(Long id, LocalDateTime createTime, List<UserDTO> users, UserDTO creator, ChatboxType chatboxType, LocalDateTime lastActiveTime) {
        this.id = id;
        this.createTime = createTime;
        this.users = users;
        this.creator = creator;
        this.chatboxType = chatboxType;
        this.lastActiveTime = lastActiveTime;
    }

    public Long getId(){
        return id;
    }
    public LocalDateTime getCreateTime(){
        return createTime;
    }
    public List<UserDTO> getUsers(){
        return users;
    }
    public UserDTO getCreator(){
        return creator;
    }
    public ChatboxType getChatboxType(){
        return chatboxType;
    }
    public LocalDateTime getLastActiveTime(){
        return lastActiveTime;
    }

    public void setCreateTime(LocalDateTime createTime){
        this.createTime = createTime;
    }
    public void setUsers(List<UserDTO> users){
        this.users = users;
    }
    public void setCreator(UserDTO creator){
        this.creator = creator;
    }
    public void setChatboxType(ChatboxType chatboxType){
        this.chatboxType = chatboxType;
    }
    public void setLastActiveTime(LocalDateTime lastActiveTime){
        this.lastActiveTime = lastActiveTime;
    }
}
