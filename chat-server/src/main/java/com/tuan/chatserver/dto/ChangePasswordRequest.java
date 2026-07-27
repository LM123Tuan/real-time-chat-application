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
public class ChangePasswordRequest {
    @NotBlank(message = "id cannot blank!")
    private Long id;
    @NotBlank(message = "oldPassword cannot blank!")
    private String oldPassword;
    @NotBlank(message = "newPassword cannot blank!")
    private String newPassword;
}
