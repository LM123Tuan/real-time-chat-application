package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.UserRole;

public class AdminDTO extends PersonDTO{
    public AdminDTO(){
        super();
    }
    public AdminDTO(Long id, String username, boolean isActive) {
        super(id, username, UserRole.ADMIN, isActive);
    }
}
