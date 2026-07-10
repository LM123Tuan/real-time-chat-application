package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.UserRole;

public class PersonDTO {
    private Long id;
    private String username;
    private UserRole role;
    private boolean isActive;

    public PersonDTO(){}
    public PersonDTO(Long id, String username, UserRole role, boolean isActive) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.isActive = isActive;
    }

    public Long getId() {
        return this.id;
    }
    public String getUsername() {
        return this.username;
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
    public void setRole(UserRole role) {
        this.role = role;
    }
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
