package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.GroupChatDTO;
import com.tuan.chatserver.dto.MyProfileDTO;
import com.tuan.chatserver.dto.UserSummaryDTO;
import com.tuan.chatserver.entity.GroupChat;
import com.tuan.chatserver.entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class GroupChatMapper {
    private static Set<UserSummaryDTO> mapUsers(Set<User> users) {
        Set<UserSummaryDTO> dtos = new HashSet<>();
        for (User user : users) {
            dtos.add(UserMapper.mapUserToUserSummaryDTO(user));
        }
        return dtos;
    }
    public static GroupChatDTO mapGroupChatToGroupChatDTO(GroupChat groupChat) {
        Long id = groupChat.getId();
        Set<User> users = groupChat.getUsers();
        Set<UserSummaryDTO> userDTOS = mapUsers(users);
        Set<User> leaders = groupChat.getLeaders();
        Set<UserSummaryDTO> leaderDTOS = mapUsers(leaders);
        Set<User> viceLeaders = groupChat.getViceLeaders();
        Set<UserSummaryDTO> viceLeaderDTOS = mapUsers(viceLeaders);
        String name = groupChat.getName();
        boolean isActive = groupChat.isActive();
        LocalDateTime lastActiveTime = groupChat.getLastActiveTime();
        GroupChatDTO groupChatDTO = new GroupChatDTO(id, userDTOS, leaderDTOS, viceLeaderDTOS, name, isActive, lastActiveTime);
        return groupChatDTO;
    }
}
