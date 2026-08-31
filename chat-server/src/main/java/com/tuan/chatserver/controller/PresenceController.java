package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.PresenceBatchResponse;
import com.tuan.chatserver.dto.PresenceResponse;
import com.tuan.chatserver.service.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/presence")
public class PresenceController{
    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService){
        this.presenceService=presenceService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PresenceResponse> getOnlineStatus(@PathVariable("userId") Long userId){
        PresenceResponse response= presenceService.isOnline(userId);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<PresenceBatchResponse> getBatchedOnlineStatus(@RequestBody List<Long> userIds){
        PresenceBatchResponse responses= presenceService.getOnlineUserIds(userIds);
        return ResponseEntity.status(200).body(responses);
    }
}
