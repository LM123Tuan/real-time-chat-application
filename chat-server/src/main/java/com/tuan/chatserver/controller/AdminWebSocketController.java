package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.enums.EventType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
public class AdminWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public AdminWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/admin/stats/user")
    @PreAuthorize("!hasRole('ADMIN')")
    public void sendUserUpdate(@Payload OtherProfileDTO payload) {
        messagingTemplate.convertAndSend("/topic/admin",
                new ChatEvent<>(EventType.ADMIN_USER_STATS_UPDATED, payload));
    }

    @MessageMapping("/admin/stats/chatbox")
    @PreAuthorize("!hasRole('ADMIN')")
    public void sendChatBoxUpdate(@Payload ChatBoxDTO payload) {
        messagingTemplate.convertAndSend("/topic/admin",
                new ChatEvent<>(EventType.ADMIN_CHATBOX_STATS_UPDATED, payload));
    }

    @MessageMapping("/admin/stats/message")
    @PreAuthorize("!hasRole('ADMIN')")
    public void sendMessageUpdate(@Payload MessageDTO payload) {
        messagingTemplate.convertAndSend("/topic/admin",
                new ChatEvent<>(EventType.ADMIN_MESSAGE_STATS_UPDATED, payload));
    }
}