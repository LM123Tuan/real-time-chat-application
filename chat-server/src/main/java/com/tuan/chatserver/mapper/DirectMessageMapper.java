package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.dto.UserSummaryDTO;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class DirectMessageMapper {
    public static DirectMessageDTO mapDirectMessageToDirectMessageDTO(DirectMessage directMessage){
        Long directMessageId = directMessage.getId();
        String name = directMessage.getName();
        Set<User> users=directMessage.getUsers();
        Set<UserSummaryDTO> userDTOS =new HashSet<>();
        for(User user:users){
            UserSummaryDTO userProfile= UserMapper.mapUserToUserSummaryDTO(user);
            userDTOS.add(userProfile);
        }
        boolean isActive = directMessage.isActive();
        LocalDateTime lastActiveTime = directMessage.getLastActiveTime();
        DirectMessageDTO directMessageDTO=new DirectMessageDTO(directMessageId, name, userDTOS, isActive, lastActiveTime);
        return directMessageDTO;
    }
}
