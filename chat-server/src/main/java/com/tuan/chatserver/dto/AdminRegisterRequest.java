package com.tuan.chatserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegisterRequest {
    @NotBlank(message = "username cannot blank!")
    String username;
    @NotBlank(message = "password cannot blank!")
    String password;
}
