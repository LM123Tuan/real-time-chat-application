package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.List;

public class DirectMessageDTO extends ChatBoxDTO{
    public DirectMessageDTO() {}
    public DirectMessageDTO(Long id, LocalDateTime createTime, List<UserDTO> users, UserDTO creator, LocalDateTime lastActiveTime) {
        super(id, createTime, users, creator, ChatboxType.DIRECT_MESSAGE, lastActiveTime);
    }
}
