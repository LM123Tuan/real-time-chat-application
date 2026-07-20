package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.GroupChatDTO;
import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.GroupChat;
import com.tuan.chatserver.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupChatMapper {
    public static GroupChatDTO mapGroupChatToGroupChatDTO(GroupChat groupChat){
        Long id=groupChat.getId();
        LocalDateTime createTime=groupChat.getCreateTime();
        List<User> users=groupChat.getUsers();
        List<UserDTO> userDTOS=new ArrayList<>();
        for(User user:users){
            UserDTO userDTO=UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userDTO);
        }
        User creator=groupChat.getCreator();
        UserDTO creatorDTO=UserMapper.mapUserToUserDTO(creator);
        String name =groupChat.getName();
        LocalDateTime lastActiveTime=groupChat.getLastActiveTime();
        GroupChatDTO groupChatDTO=new GroupChatDTO(id, createTime, userDTOS, creatorDTO, name, lastActiveTime);
        return groupChatDTO;
    }
}
