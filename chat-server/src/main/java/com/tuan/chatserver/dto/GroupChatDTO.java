package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.Set;

public class GroupChatDTO extends ChatBoxDTO{
    private Set<UserSummaryDTO> leaders;
    private Set<UserSummaryDTO> viceLeaders;
    public GroupChatDTO() {}
    public GroupChatDTO(Long id, Set<UserSummaryDTO> users, Set<UserSummaryDTO> leaders, Set<UserSummaryDTO> viceLeaders, String name, boolean isActive, LocalDateTime lastActiveTime) {
        super(id, name, ChatboxType.GROUP_CHAT, isActive, lastActiveTime);
        this.leaders = leaders;
        this.viceLeaders = viceLeaders;
    }

    public Set<UserSummaryDTO> getLeaders() {
        return leaders;
    }
    public Set<UserSummaryDTO> getViceLeaders() {
        return viceLeaders;
    }

    public void setLeaders(Set<UserSummaryDTO> leaders) {
        this.leaders = leaders;
    }
    public void setViceLeaders(Set<UserSummaryDTO> viceLeaders) {
        this.viceLeaders = viceLeaders;
    }
}
