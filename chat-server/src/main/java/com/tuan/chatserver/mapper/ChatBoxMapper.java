package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.MyProfileDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class ChatBoxMapper {
    public static ChatBoxDTO mapChatBoxToChatBoxDTO(ChatBox chatBox) {
        Long chatBoxId = chatBox.getId();
        LocalDateTime timestamp = chatBox.getCreateTime();
        Set<User> users=chatBox.getUsers();
        Set<MyProfileDTO> myProfileDTOS =new HashSet<>();
        for(User user:users){
            MyProfileDTO userProfile= UserMapper.mapUserToUserDTO(user);
            myProfileDTOS.add(userProfile);
        }
        ChatboxType chatboxType=chatBox.getChatboxType();
        boolean isActive=chatBox.isActive();
        LocalDateTime lastActiveTime = chatBox.getLastActiveTime();
        ChatBoxDTO chatBoxDTO=new ChatBoxDTO(chatBoxId, timestamp, myProfileDTOS, chatboxType, isActive, lastActiveTime);
        return chatBoxDTO;
    }
}
