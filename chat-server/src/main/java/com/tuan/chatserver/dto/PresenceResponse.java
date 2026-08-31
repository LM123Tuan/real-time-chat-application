package com.tuan.chatserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresenceResponse {
    private Long userId;
    private String status;
}
