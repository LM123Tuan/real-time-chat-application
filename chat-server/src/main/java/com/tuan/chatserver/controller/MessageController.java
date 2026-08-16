package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.*;
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

    @PostMapping("/chats/{chatBoxId}/messages/load")
    public ResponseEntity<CursorPaginationResponse<List<MessageDTO>>> loadMessages(
            Authentication authentication,
            @PathVariable Long chatBoxId,
            @RequestBody CursorPaginationRequest request) {

        Long requesterId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        CursorPaginationResponse<List<MessageDTO>> dtos = messageService.loadAllMessagesForChatBox(requesterId, chatBoxId, request);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/chats/{chatBoxId}/messages/filter")
    public ResponseEntity<CursorPaginationResponse<List<MessageDTO>>> getMessages(
            Authentication authentication,
            @PathVariable Long chatBoxId,
            @ModelAttribute SearchMessageRequest searchRequest,
            @RequestBody CursorPaginationRequest request) {

        Long requesterId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        CursorPaginationResponse<List<MessageDTO>> messages = messageService.findMessages(
                requesterId, chatBoxId, searchRequest, request);
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
    public ResponseEntity<Void> updateMessageStatus(
            Authentication authentication,
            @PathVariable String messageId) {

        Long requesterId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        messageService.updateMessageStatus(requesterId, messageId);
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