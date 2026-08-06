package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;

public class ChatBoxMapper {
    public static ChatBoxDTO mapChatBoxToChatBoxDTO(ChatBox chatBox) {
        Long chatBoxId = chatBox.getId();
        String name=chatBox.getName();
        ChatboxType chatboxType=chatBox.getChatboxType();
        boolean isActive=chatBox.isActive();
        LocalDateTime lastActiveTime = chatBox.getLastActiveTime();
        ChatBoxDTO chatBoxDTO=new ChatBoxDTO(chatBoxId, name, chatboxType, isActive, lastActiveTime);
        return chatBoxDTO;
    }
}
