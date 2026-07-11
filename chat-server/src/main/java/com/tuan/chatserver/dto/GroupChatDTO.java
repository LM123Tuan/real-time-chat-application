package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.List;

public class GroupChatDTO extends ChatBoxDTO{
    private String name;
    public GroupChatDTO() {}
    public GroupChatDTO(Long id, LocalDateTime createTime, List<UserDTO> users, UserDTO creator, String name) {
        super(id, createTime, users, creator, ChatboxType.GROUP_CHAT);
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
