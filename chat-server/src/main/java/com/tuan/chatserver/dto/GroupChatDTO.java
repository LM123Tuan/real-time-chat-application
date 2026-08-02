package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.Set;

public class GroupChatDTO extends ChatBoxDTO{
    private String name;
    private Set<MyProfileDTO> leaders;
    private Set<MyProfileDTO> viceLeaders;
    public GroupChatDTO() {}
    public GroupChatDTO(Long id, LocalDateTime createTime, Set<MyProfileDTO> users, Set<MyProfileDTO> leaders, Set<MyProfileDTO> viceLeaders, String name, boolean isActive, LocalDateTime lastActiveTime) {
        super(id, createTime, users, ChatboxType.GROUP_CHAT, isActive, lastActiveTime);
        this.name = name;
        this.leaders = leaders;
        this.viceLeaders = viceLeaders;
    }

    public String getName() {
        return name;
    }
    public Set<MyProfileDTO> getLeaders() {
        return leaders;
    }
    public Set<MyProfileDTO> getViceLeaders() {
        return viceLeaders;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setLeaders(Set<MyProfileDTO> leaders) {
        this.leaders = leaders;
    }
    public void setViceLeaders(Set<MyProfileDTO> viceLeaders) {
        this.viceLeaders = viceLeaders;
    }
}
