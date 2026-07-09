package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class GroupChat extends ChatBox{
    public GroupChat() {}
    public GroupChat(String id, LocalDateTime createTime, ArrayList<User> users, User creator) {
        super(id, createTime, users, creator, ChatboxType.GROUP_CHAT);
    }
}
