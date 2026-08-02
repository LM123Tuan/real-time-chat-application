package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.Set;

public class DirectMessageDTO extends ChatBoxDTO{
    public DirectMessageDTO() {}
    public DirectMessageDTO(Long id, LocalDateTime createTime, Set<MyProfileDTO> users, boolean isActive, LocalDateTime lastActiveTime) {
        super(id, createTime, users, ChatboxType.DIRECT_MESSAGE, isActive, lastActiveTime);
    }
}
