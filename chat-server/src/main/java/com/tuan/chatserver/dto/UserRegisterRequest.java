package com.tuan.chatserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserRegisterRequest {
    @NotBlank(message = "fullname cannot blank!")
    private String fullname;
    @NotBlank(message = "username cannot blank!")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]{5,20}$",
            message = "Username must contain only letters, numbers, and underscores, and be between 5 and 20 characters long"
    )
    private String username;
    @NotBlank(message = "email cannot blank!")
    @Email(message = "Invalid email format!")
    private String email;
    @NotBlank(message = "password cannot blank!")
    private String password;
    private String phone;

    public UserRegisterRequest(){}

    public UserRegisterRequest(String fullname, String username, String email, String password, String phone) {
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    public String getFullname() {
        return fullname;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public String getPhone() {
        return phone;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
}
