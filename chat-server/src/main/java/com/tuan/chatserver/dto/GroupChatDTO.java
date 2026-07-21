package com.tuan.chatserver.dto;

import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class GroupChatDTO extends ChatBoxDTO{
    private String name;
    private Set<UserDTO> leaders;
    private Set<UserDTO> viceLeaders;
    public GroupChatDTO() {}
    public GroupChatDTO(Long id, LocalDateTime createTime, Set<UserDTO> users, Set<UserDTO> leaders, Set<UserDTO> viceLeaders, String name, boolean isActive, LocalDateTime lastActiveTime) {
        super(id, createTime, users, ChatboxType.GROUP_CHAT, isActive, lastActiveTime);
        this.name = name;
        this.leaders = leaders;
        this.viceLeaders = viceLeaders;
    }

    public String getName() {
        return name;
    }
    public Set<UserDTO> getLeaders() {
        return leaders;
    }
    public Set<UserDTO> getViceLeaders() {
        return viceLeaders;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setLeaders(Set<UserDTO> leaders) {
        this.leaders = leaders;
    }
    public void setViceLeaders(Set<UserDTO> viceLeaders) {
        this.viceLeaders = viceLeaders;
    }
}
