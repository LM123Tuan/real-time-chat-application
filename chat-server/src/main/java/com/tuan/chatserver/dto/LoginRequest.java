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
public class LoginRequest {
    @NotBlank(message = "identifier cannot blank!")
    private String identifier;
    @NotBlank(message = "password cannot blank!")
    private String password;
}
