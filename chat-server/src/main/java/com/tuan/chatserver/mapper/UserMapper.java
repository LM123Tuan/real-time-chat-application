package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.MyProfileDTO;
import com.tuan.chatserver.dto.OtherProfileDTO;
import com.tuan.chatserver.dto.UserSummaryDTO;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.UserRole;

public class UserMapper {
    public static MyProfileDTO mapUserToUserDTO(User user){
        Long id=user.getId();
        String userName=user.getUsername();
        boolean isActive=user.isActive();
        String fullname=user.getFullname();
        String email=user.getEmail();
        String phone=user.getPhone();
        MyProfileDTO myProfileDTO = new MyProfileDTO(id, fullname, userName, email, phone, isActive);
        return myProfileDTO;
    }

    public static OtherProfileDTO mapUserToOtherUserDTO(User user){
        Long id=user.getId();
        String username=user.getUsername();
        UserRole role=user.getRole();
        boolean isActive=user.isActive();
        String email=user.getEmail();

        return new OtherProfileDTO(id, username, role, isActive, email);
    }

    public static UserSummaryDTO mapUserToUserSummaryDTO(User user){
        Long id=user.getId();
        String username=user.getUsername();
        return new UserSummaryDTO(id, username);
    }
}
