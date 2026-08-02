package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OtherProfileDTO extends PersonDTO{
    private String email;

    public OtherProfileDTO(Long id, String username, UserRole role, boolean isActive, String email) {
        super(id, username, role, isActive);
        this.email = email;
    }
}
