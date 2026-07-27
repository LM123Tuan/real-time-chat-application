package com.tuan.chatserver.dto;

import com.tuan.chatserver.entity.RefreshToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private PersonDTO personDTO;
    private String accessToken;
    private String refreshToken;
}
