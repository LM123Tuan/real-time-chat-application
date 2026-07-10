package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.UserRole;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin")
public class Admin extends Person{
    public Admin(){
        super();
    }
    public Admin(String username, String password, boolean isActive) {
        super(username, password, UserRole.ADMIN, isActive);
    }
}
