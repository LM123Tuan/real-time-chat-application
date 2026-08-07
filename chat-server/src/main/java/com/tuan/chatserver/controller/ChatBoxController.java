package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.CursorPaginationRequest;
import com.tuan.chatserver.dto.CursorPaginationResponse;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.ChatBoxService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chats")
public class ChatBoxController {
    private final ChatBoxService chatBoxService;

    public ChatBoxController(ChatBoxService chatBoxService){
        this.chatBoxService=chatBoxService;
    }

    @PostMapping
    public ResponseEntity<CursorPaginationResponse<List<ChatBoxDTO>>> getAllChatBoxes(Authentication authentication,
                                                                                            @Valid @RequestBody CursorPaginationRequest request){
        Long id = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        CursorPaginationResponse<List<ChatBoxDTO>> chatBoxes= chatBoxService.getAllChatboxesForUser(id, request);
        return ResponseEntity.ok(chatBoxes);
    }
}
