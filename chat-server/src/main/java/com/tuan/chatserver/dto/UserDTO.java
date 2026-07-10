package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.UserRole;

public class UserDTO extends PersonDTO {
    private String fullname;
    private String phone;
    private String email;

    public UserDTO() {
        super();
    }
    public UserDTO(Long id, String fullname, String username, String email, String phone, boolean isActive) {
        super(id, username, UserRole.USER, isActive);
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
