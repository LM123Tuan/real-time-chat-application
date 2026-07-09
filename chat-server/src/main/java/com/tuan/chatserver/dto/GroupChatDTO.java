package com.tuan.chatserver.dto;

import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class GroupChatDTO extends ChatBoxDTO{
    public GroupChatDTO() {}
    public GroupChatDTO(String id, LocalDateTime createTime, ArrayList<User> users, User creator) {
        super(id, createTime, users, creator, ChatboxType.GROUP_CHAT);
    }
}
