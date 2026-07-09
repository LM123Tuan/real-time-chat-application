package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.UserRole;

public class User extends Person{
    private String fullname;
    private String phone;
    private String email;

    public User(){
        super();
    }
    public User(String id, String fullname, String username, String email, String password, String phone, boolean isActive) {
        super(id, username, password, UserRole.USER, isActive);
        this.fullname = fullname;
        this.phone = phone;
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }
    public String getPhone() {
        return phone;
    }
    public String getEmail() {
        return email;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public void setEmail(String email) {
        this.email = email;
    }
}
