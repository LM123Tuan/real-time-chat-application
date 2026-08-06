package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.ChatboxType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class DirectMessageDTO extends ChatBoxDTO{
    private Set<UserSummaryDTO> users;

    public DirectMessageDTO() {}
    public DirectMessageDTO(Long id, String name, Set<UserSummaryDTO> users, boolean isActive, LocalDateTime lastActiveTime) {
        super(id, name, ChatboxType.DIRECT_MESSAGE, isActive, lastActiveTime);
        this.users=users;
    }
}
