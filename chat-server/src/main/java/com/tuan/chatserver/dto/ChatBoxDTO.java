package com.tuan.chatserver.dto;

import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class ChatBoxDTO {
    private String id;
    private LocalDateTime createTime;
    private ArrayList<User> users;
    private User creator;
    private ChatboxType chatboxType;

    public ChatBoxDTO() {}
    public ChatBoxDTO(String id, LocalDateTime createTime, ArrayList<User> users, User creator, ChatboxType chatboxType) {
        this.id = id;
        this.createTime = createTime;
        this.users = users;
        this.creator = creator;
        this.chatboxType = chatboxType;
    }

    public String getId(){
        return id;
    }
    public LocalDateTime getCreateTime(){
        return createTime;
    }
    public ArrayList<User> getUsers(){
        return users;
    }
    public User getCreator(){
        return creator;
    }
    public ChatboxType getChatboxType(){
        return chatboxType;
    }

    public void setCreateTime(LocalDateTime createTime){
        this.createTime = createTime;
    }
    public void setUsers(ArrayList<User> users){
        this.users = users;
    }
    public void setCreator(User creator){
        this.creator = creator;
    }
    public void setChatboxType(ChatboxType chatboxType){
        this.chatboxType = chatboxType;
    }
}
