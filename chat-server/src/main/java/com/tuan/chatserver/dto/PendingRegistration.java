package com.tuan.chatserver.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {
    @Email
    private String email;
    private String username;
    private String hashedPassword;
    private String fullname;
    private String phone;
}
