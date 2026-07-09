package com.tuan.chatserver.dto;

import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class DirectMessageDTO extends ChatBoxDTO{
    public DirectMessageDTO() {}
    public DirectMessageDTO(String id, LocalDateTime createTime, ArrayList<User> users, User creator) {
        super(id, createTime, users, creator, ChatboxType.DIRECT_MESSAGE);
    }
}
