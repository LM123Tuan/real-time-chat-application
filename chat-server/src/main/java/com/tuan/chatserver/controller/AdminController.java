package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.AdminService;
import com.tuan.chatserver.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final PresenceService presenceService;

    public AdminController(AdminService adminService,
                           PresenceService presenceService) {
        this.adminService = adminService;
        this.presenceService=presenceService;
    }

    private Long extractRequesterId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
    }

    //ADMIN

    @PostMapping("/register")
    public ResponseEntity<Void> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        adminService.registerAdmin(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<AdminDTO> findAdminByUsername(@RequestParam String username) {
        AdminDTO adminDTO = adminService.findAdminByUsername(username);
        return ResponseEntity.ok(adminDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminDTO> findAdminById(@PathVariable Long id) {
        AdminDTO adminDTO = adminService.findAdminById(id);
        return ResponseEntity.ok(adminDTO);
    }

    @GetMapping("/me")
    public ResponseEntity<AdminDTO> getAdminProfile(Authentication authentication) {
        Long requesterId = extractRequesterId(authentication);
        AdminDTO adminDTO = adminService.getAdminProfile(requesterId);
        return ResponseEntity.ok(adminDTO);
    }

    //USER

    @PostMapping("/users/search")
    public ResponseEntity<CursorPaginationResponse<List<OtherProfileDTO>>> findAllUsers(@RequestParam(required = false) String keyword,
                                                                                        @RequestBody CursorPaginationRequest request) {
        CursorPaginationResponse<List<OtherProfileDTO>> users = (keyword == null || keyword.isBlank())
                ? adminService.findAllUsers(request)
                : adminService.findUserByUsernameContaining(keyword, request);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/{id}/active-status")
    public ResponseEntity<Void> changeActiveStatusForUser(@PathVariable Long id, @RequestParam boolean status) {
        adminService.changeActiveStatusForUser(id, status);
        return ResponseEntity.noContent().build();
    }

    //CHATBOX

    @PostMapping("/chatboxes/search")
    public ResponseEntity<CursorPaginationResponse<List<ChatBoxDTO>>> getAllChatBox(@RequestBody CursorPaginationRequest request) {
        CursorPaginationResponse<List<ChatBoxDTO>> chatBoxes = adminService.getAllChatBox(request);
        return ResponseEntity.ok(chatBoxes);
    }

    @PostMapping("/users/{userId}/chatboxes/search")
    public ResponseEntity<CursorPaginationResponse<List<ChatBoxDTO>>> getAllUserChatBox(@PathVariable Long userId,
                                                                                        @RequestBody CursorPaginationRequest request) {
        CursorPaginationResponse<List<ChatBoxDTO>> chatBoxes = adminService.getAllUserChatBox(userId, request);
        return ResponseEntity.ok(chatBoxes);
    }

    //STATISTICS

    @GetMapping("/stats/users")
    public ResponseEntity<Long> countUsers(@RequestParam(required = false) Boolean isActive) {
        Long count = (isActive == null)
                ? adminService.countUsers()
                : adminService.countUsersByActiveStatus(isActive);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/chatboxes")
    public ResponseEntity<Long> countChatBoxes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long count = (startTime == null || endTime == null)
                ? adminService.countChatBoxes()
                : adminService.countChatBoxesByLastActiveTimeBetween(startTime, endTime);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/messages")
    public ResponseEntity<Long> countMessages(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long count = (startTime == null || endTime == null)
                ? adminService.countMessages()
                : adminService.countMessagesByTimestampBetween(startTime, endTime);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/online-users")
    public ResponseEntity<Long> countOnlineUsers() {
        Long count = presenceService.getOnlineCount();
        return ResponseEntity.ok(count);
    }
}