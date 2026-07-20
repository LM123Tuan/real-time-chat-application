package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatBoxMapper {
    public static ChatBoxDTO mapChatBoxToChatBoxDTO(ChatBox chatBox) {
        Long chatBoxId = chatBox.getId();
        LocalDateTime timestamp = chatBox.getCreateTime();
        List<User> users=chatBox.getUsers();
        List<UserDTO> userDTOS=new ArrayList<>();
        for(User user:users){
            UserDTO userProfile= UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userProfile);
        }
        User creator = chatBox.getCreator();
        UserDTO creatorDTO = UserMapper.mapUserToUserDTO(creator);
        ChatboxType chatboxType=chatBox.getChatboxType();
        LocalDateTime lastActiveTime = chatBox.getLastActiveTime();
        ChatBoxDTO chatBoxDTO=new ChatBoxDTO(chatBoxId, timestamp, userDTOS, creatorDTO, chatboxType, lastActiveTime);
        return chatBoxDTO;
    }
}
