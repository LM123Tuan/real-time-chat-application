package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class DirectMessage extends ChatBox{
    public DirectMessage() {}
    public DirectMessage(String id, LocalDateTime createTime, ArrayList<User> users, User creator) {
        super(id, createTime, users, creator, ChatboxType.DIRECT_MESSAGE);
    }
}
