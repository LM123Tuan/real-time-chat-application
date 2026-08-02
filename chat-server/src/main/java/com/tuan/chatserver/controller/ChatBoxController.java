package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.ChatBoxService;
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
    public ResponseEntity<List<ChatBoxDTO>> getAllChatBoxes(Authentication authentication){
        Long id = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        List<ChatBoxDTO> chatBoxes= chatBoxService.getAllChatboxesForUser(id);
        return ResponseEntity.ok(chatBoxes);
    }
}
