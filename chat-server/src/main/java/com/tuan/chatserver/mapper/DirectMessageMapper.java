package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.ChatboxType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DirectMessageMapper {
    public static DirectMessageDTO mapDirectMessageToDirectMessageDTO(DirectMessage directMessage){
        Long directMessageId = directMessage.getId();
        LocalDateTime timestamp = directMessage.getCreateTime();
        List<User> users=directMessage.getUsers();
        List<UserDTO> userDTOS=new ArrayList<>();
        for(User user:users){
            UserDTO userProfile= UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userProfile);
        }
        User creator = directMessage.getCreator();
        UserDTO creatorDTO = UserMapper.mapUserToUserDTO(creator);
        LocalDateTime lastActiveTime = directMessage.getLastActiveTime();
        DirectMessageDTO directMessageDTO=new DirectMessageDTO(directMessageId, timestamp, userDTOS, creatorDTO, lastActiveTime);
        return directMessageDTO;
    }
}
