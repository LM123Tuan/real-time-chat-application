package com.tuan.chatserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @NotBlank(message = "fullname cannot blank!")
    String fullname;
    @NotBlank(message = "username cannot blank!")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]{5,20}$",
            message = "Username must contain only letters, numbers, and underscores, and be between 5 and 20 characters long"
    )
    String username;
    @NotBlank(message = "email cannot blank!")
    @Email(message = "Invalid email format!")
    String email;
    String phone;
}
