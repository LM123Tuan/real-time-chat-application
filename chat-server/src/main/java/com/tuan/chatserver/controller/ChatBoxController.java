package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.CursorPaginationRequest;
import com.tuan.chatserver.dto.CursorPaginationResponse;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.ChatBoxService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chats")
public class ChatBoxController {
    private final ChatBoxService chatBoxService;

    public ChatBoxController(ChatBoxService chatBoxService){
        this.chatBoxService=chatBoxService;
    }

    @GetMapping
    public ResponseEntity<CursorPaginationResponse<List<ChatBoxDTO>, Long>> getAllChatBoxes(Authentication authentication, @Valid CursorPaginationRequest request){
        Long id = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        CursorPaginationResponse<List<ChatBoxDTO>, Long> chatBoxes= chatBoxService.getAllChatboxesForUser(id, request);
        return ResponseEntity.ok(chatBoxes);
    }
}
