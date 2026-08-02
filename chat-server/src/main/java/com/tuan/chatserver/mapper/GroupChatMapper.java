package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.GroupChatDTO;
import com.tuan.chatserver.dto.MyProfileDTO;
import com.tuan.chatserver.entity.GroupChat;
import com.tuan.chatserver.entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class GroupChatMapper {
    private static Set<MyProfileDTO> mapUsers(Set<User> users) {
        Set<MyProfileDTO> dtos = new HashSet<>();
        for (User user : users) {
            dtos.add(UserMapper.mapUserToUserDTO(user));
        }
        return dtos;
    }
    public static GroupChatDTO mapGroupChatToGroupChatDTO(GroupChat groupChat) {
        Long id = groupChat.getId();
        LocalDateTime createTime = groupChat.getCreateTime();
        Set<User> users = groupChat.getUsers();
        Set<MyProfileDTO> myProfileDTOS = mapUsers(users);
        Set<User> leaders = groupChat.getLeaders();
        Set<MyProfileDTO> leaderDTOS = mapUsers(leaders);
        Set<User> viceLeaders = groupChat.getViceLeaders();
        Set<MyProfileDTO> viceLeaderDTOS = mapUsers(viceLeaders);
        String name = groupChat.getName();
        boolean isActive = groupChat.isActive();
        LocalDateTime lastActiveTime = groupChat.getLastActiveTime();
        GroupChatDTO groupChatDTO = new GroupChatDTO(id, createTime, myProfileDTOS, leaderDTOS, viceLeaderDTOS, name, isActive, lastActiveTime);
        return groupChatDTO;
    }
}
