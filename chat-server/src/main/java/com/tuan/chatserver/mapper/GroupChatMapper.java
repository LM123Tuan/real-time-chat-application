package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.GroupChatDTO;
import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.GroupChat;
import com.tuan.chatserver.entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class GroupChatMapper {
    private static Set<UserDTO> mapUsers(Set<User> users) {
        Set<UserDTO> dtos = new HashSet<>();
        for (User user : users) {
            dtos.add(UserMapper.mapUserToUserDTO(user));
        }
        return dtos;
    }
    public static GroupChatDTO mapGroupChatToGroupChatDTO(GroupChat groupChat) {
        Long id = groupChat.getId();
        LocalDateTime createTime = groupChat.getCreateTime();
        Set<User> users = groupChat.getUsers();
        Set<UserDTO> userDTOS = mapUsers(users);
        Set<User> leaders = groupChat.getLeaders();
        Set<UserDTO> leaderDTOS = mapUsers(leaders);
        Set<User> viceLeaders = groupChat.getViceLeaders();
        Set<UserDTO> viceLeaderDTOS = mapUsers(viceLeaders);
        String name = groupChat.getName();
        boolean isActive = groupChat.isActive();
        LocalDateTime lastActiveTime = groupChat.getLastActiveTime();
        GroupChatDTO groupChatDTO = new GroupChatDTO(id, createTime, userDTOS, leaderDTOS, viceLeaderDTOS, name, isActive, lastActiveTime);
        return groupChatDTO;
    }
}
