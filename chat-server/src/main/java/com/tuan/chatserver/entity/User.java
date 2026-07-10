package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User extends Person{
    @Column(nullable = false)
    private String fullname;
    @Column()
    private String phone;
    @Column(unique = true, nullable = false)
    private String email;

    public User(){
        super();
    }
    public User(String fullname, String username, String email, String password, String phone, boolean isActive) {
        super(username, password, UserRole.USER, isActive);
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
