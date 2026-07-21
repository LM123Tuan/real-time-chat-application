package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.User;

public class UserMapper {
    public static UserDTO mapUserToUserDTO(User user){
        Long id=user.getId();
        String userName=user.getUsername();
        boolean isActive=user.isActive();
        String fullname=user.getFullname();
        String email=user.getEmail();
        String phone=user.getPhone();
        UserDTO userDTO = new UserDTO(id, fullname, userName, email, phone, isActive);
        return userDTO;
    }
}
