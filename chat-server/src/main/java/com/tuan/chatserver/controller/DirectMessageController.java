package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.DirectMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chats/dm")
public class DirectMessageController {
    private final DirectMessageService directMessageService;

    public DirectMessageController(DirectMessageService directMessageService){
        this.directMessageService=directMessageService;
    }

    @GetMapping
    public ResponseEntity<List<DirectMessageDTO>> getAllPrivateChat(Authentication authentication){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        List<DirectMessageDTO> dtos = directMessageService.getAllChatByUserId(userId);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DirectMessageDTO> getChatBetweenTwoUsers(Authentication authentication, @PathVariable Long id){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        DirectMessageDTO directMessageDTO = directMessageService.getChatBetweenTwoUsersByChatBoxId(id, userId);
        return ResponseEntity.ok(directMessageDTO);
    }

    @PostMapping
    public ResponseEntity<DirectMessageDTO> createPrivateChat(Authentication authentication, @RequestBody Long otherUserId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        DirectMessageDTO directMessageDTO = directMessageService.createDirectMessage(userId, otherUserId);
        return ResponseEntity.ok(directMessageDTO);
    }
}
