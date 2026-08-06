package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.MessageDTO;
import com.tuan.chatserver.dto.SearchMessageRequest;
import com.tuan.chatserver.dto.SendMessageRequest;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/chats/{chatBoxId}/messages")
    public ResponseEntity<List<MessageDTO>> loadMessages(
            Authentication authentication,
            @PathVariable Long chatBoxId) {

        Long requesterId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        List<MessageDTO> dtos = messageService.loadAllMessagesForChatBox(requesterId, chatBoxId);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/chats/{chatBoxId}/messages/filter")
    public ResponseEntity<List<MessageDTO>> getMessages(
            Authentication authentication,
            @PathVariable Long chatBoxId,
            @ModelAttribute SearchMessageRequest request) {

        Long requesterId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        List<MessageDTO> messages = messageService.findMessages(requesterId, chatBoxId, request);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/chats/{chatBoxId}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            Authentication authentication,
            @PathVariable Long chatBoxId,
            @Valid @RequestBody SendMessageRequest request) {

        Long senderId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        MessageDTO dto = messageService.sendMessage(senderId, chatBoxId, request.getContent());
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/messages/{messageId}/status")
    public ResponseEntity<Void> updateMessageStatus(@PathVariable String messageId) {
        messageService.updateMessageStatus(messageId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/messages/{messageId}/recall")
    public ResponseEntity<Void> recallMessage(
            Authentication authentication,
            @PathVariable String messageId) {

        Long senderId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        messageService.recallMessage(senderId, messageId);
        return ResponseEntity.noContent().build();
    }
}