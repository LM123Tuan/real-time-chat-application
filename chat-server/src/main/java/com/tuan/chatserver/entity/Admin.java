package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.UserRole;

public class Admin extends Person{
    public Admin(){
        super();
    }
    public Admin(String id, String username, String password, UserRole role, boolean isActive) {
        super(id, username, password, UserRole.ADMIN, isActive);
    }
}
