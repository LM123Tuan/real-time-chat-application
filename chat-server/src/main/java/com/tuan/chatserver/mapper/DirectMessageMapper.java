package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.dto.MyProfileDTO;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class DirectMessageMapper {
    public static DirectMessageDTO mapDirectMessageToDirectMessageDTO(DirectMessage directMessage){
        Long directMessageId = directMessage.getId();
        LocalDateTime timestamp = directMessage.getCreateTime();
        Set<User> users=directMessage.getUsers();
        Set<MyProfileDTO> myProfileDTOS =new HashSet<>();
        for(User user:users){
            MyProfileDTO userProfile= UserMapper.mapUserToUserDTO(user);
            myProfileDTOS.add(userProfile);
        }
        boolean isActive = directMessage.isActive();
        LocalDateTime lastActiveTime = directMessage.getLastActiveTime();
        DirectMessageDTO directMessageDTO=new DirectMessageDTO(directMessageId, timestamp, myProfileDTOS, isActive, lastActiveTime);
        return directMessageDTO;
    }
}
