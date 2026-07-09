package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.UserRole;

public abstract class Person {
    private String id;
    private String username;
    private String password;
    private UserRole role;
    private boolean isActive;

    public Person(){}
    public Person(String id, String username, String password, UserRole role, boolean isActive) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
    }

    public String getId() {
        return this.id;
    }
    public String getUsername() {
        return this.username;
    }
    public String getPassword() {
        return this.password;
    }
    public UserRole getRole() {
        return this.role;
    }
    public boolean isActive() {
        return this.isActive;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setRole(UserRole role) {
        this.role = role;
    }
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}