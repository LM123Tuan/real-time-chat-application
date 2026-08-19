package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.ChatEvent;
import com.tuan.chatserver.dto.MessageDTO;
import com.tuan.chatserver.dto.SendMessageRequest;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.EventType;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.ChatBoxService;
import com.tuan.chatserver.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final ChatBoxService chatBoxService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   ChatBoxService chatBoxService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.chatBoxService = chatBoxService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send/{chatBoxId}")
    public void sendMessage(@DestinationVariable Long chatBoxId,
                            @Payload SendMessageRequest request,
                            Authentication authentication) {
        Long senderId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();

        MessageDTO dto = messageService.sendMessage(senderId, chatBoxId, request.getContent());

        messagingTemplate.convertAndSend(
                "/topic/chatbox/" + chatBoxId,
                new ChatEvent<>(EventType.MESSAGE_NEW, dto));

        ChatBox chatBox = chatBoxService.getChatBoxWithUsers(chatBoxId);
        for (User member : chatBox.getUsers()) {
            if (member.getId().equals(senderId)) {
                continue;
            }
            messagingTemplate.convertAndSendToUser(
                    member.getId().toString(),
                    "/queue/notifications",
                    new ChatEvent<>(EventType.MESSAGE_NEW, dto));
        }
    }

    @MessageMapping("/chat.status/received/{chatBoxId}")
    public void markMessageAsReceived(@DestinationVariable Long chatBoxId,
                                      @Payload String messageId,
                                      Authentication authentication) {
        Long requesterId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();

        messageService.markMessageAsReceived(requesterId, messageId);

        messagingTemplate.convertAndSend(
                "/topic/chatbox/" + chatBoxId,
                new ChatEvent<>(EventType.MESSAGE_STATUS_UPDATED, messageId));
    }

    @MessageMapping("/chat.status/seen/{chatBoxId}")
    public void markMessageAsSeen(@DestinationVariable Long chatBoxId,
                                  @Payload String messageId,
                                  Authentication authentication) {
        Long requesterId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();

        messageService.markMessageAsSeen(requesterId, messageId);

        messagingTemplate.convertAndSend(
                "/topic/chatbox/" + chatBoxId,
                new ChatEvent<>(EventType.MESSAGE_STATUS_UPDATED, messageId));
    }

    @MessageMapping("/chat.recall/{chatBoxId}")
    public void recallMessage(@DestinationVariable Long chatBoxId,
                              @Payload String messageId,
                              Authentication authentication) {
        Long senderId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        messageService.recallMessage(senderId, messageId);

        messagingTemplate.convertAndSend(
                "/topic/chatbox/" + chatBoxId,
                new ChatEvent<>(EventType.MESSAGE_RECALLED, messageId));

        ChatBox chatBox = chatBoxService.getChatBoxWithUsers(chatBoxId);
        for (User member : chatBox.getUsers()) {
            if (member.getId().equals(senderId)) {
                continue;
            }
            messagingTemplate.convertAndSendToUser(
                    member.getId().toString(),
                    "/queue/notifications",
                    new ChatEvent<>(EventType.MESSAGE_RECALLED, messageId));
        }
    }
}