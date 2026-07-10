package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.List;

public class GroupChatDTO extends ChatBoxDTO{
    public GroupChatDTO() {}
    public GroupChatDTO(Long id, LocalDateTime createTime, List<UserDTO> users, UserDTO creator) {
        super(id, createTime, users, creator, ChatboxType.GROUP_CHAT);
    }
}
