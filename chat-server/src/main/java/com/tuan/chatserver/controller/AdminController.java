package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.AdminService;
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

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
    public ResponseEntity<AdminDTO> findAdminByUsername(Authentication authentication, @RequestParam String username) {
        Long requesterId = extractRequesterId(authentication);
        AdminDTO adminDTO = adminService.findAdminByUsername(requesterId, username);
        return ResponseEntity.ok(adminDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminDTO> findAdminById(Authentication authentication, @PathVariable Long id) {
        Long requesterId = extractRequesterId(authentication);
        AdminDTO adminDTO = adminService.findAdminById(requesterId, id);
        return ResponseEntity.ok(adminDTO);
    }

    @GetMapping("/me")
    public ResponseEntity<AdminDTO> getAdminProfile(Authentication authentication) {
        Long requesterId = extractRequesterId(authentication);
        AdminDTO adminDTO = adminService.getAdminProfile(requesterId, requesterId);
        return ResponseEntity.ok(adminDTO);
    }

    //USER

    @PostMapping("/users/search")
    public ResponseEntity<CursorPaginationResponse<List<OtherProfileDTO>>> findAllUsers(Authentication authentication,
                                                                                        @RequestParam(required = false) String keyword,
                                                                                        @RequestBody CursorPaginationRequest request) {
        Long requesterId = extractRequesterId(authentication);
        CursorPaginationResponse<List<OtherProfileDTO>> users = (keyword == null || keyword.isBlank())
                ? adminService.findAllUsers(requesterId, request)
                : adminService.findUserByUsernameContaining(requesterId, keyword, request);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/{id}/active-status")
    public ResponseEntity<Void> changeActiveStatusForUser(Authentication authentication, @PathVariable Long id) {
        Long requesterId = extractRequesterId(authentication);
        adminService.changeActiveStatusForUser(requesterId, id);
        return ResponseEntity.noContent().build();
    }

    //CHATBOX

    @PostMapping("/chatboxes/search")
    public ResponseEntity<CursorPaginationResponse<List<ChatBoxDTO>>> getAllChatBox(Authentication authentication,
                                                                                    @RequestBody CursorPaginationRequest request) {
        Long requesterId = extractRequesterId(authentication);
        CursorPaginationResponse<List<ChatBoxDTO>> chatBoxes = adminService.getAllChatBox(requesterId, request);
        return ResponseEntity.ok(chatBoxes);
    }

    @PostMapping("/users/{userId}/chatboxes/search")
    public ResponseEntity<CursorPaginationResponse<List<ChatBoxDTO>>> getAllUserChatBox(Authentication authentication,
                                                                                        @PathVariable Long userId,
                                                                                        @RequestBody CursorPaginationRequest request) {
        Long requesterId = extractRequesterId(authentication);
        CursorPaginationResponse<List<ChatBoxDTO>> chatBoxes = adminService.getAllUserChatBox(requesterId, userId, request);
        return ResponseEntity.ok(chatBoxes);
    }

    //STATISTICS

    @GetMapping("/stats/users")
    public ResponseEntity<Long> countUsers(Authentication authentication, @RequestParam(required = false) Boolean isActive) {
        Long requesterId = extractRequesterId(authentication);
        Long count = (isActive == null)
                ? adminService.countUsers(requesterId)
                : adminService.countUsersByActiveStatus(requesterId, isActive);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/chatboxes")
    public ResponseEntity<Long> countChatBoxes(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long requesterId = extractRequesterId(authentication);
        Long count = (startTime == null || endTime == null)
                ? adminService.countChatBoxes(requesterId)
                : adminService.countChatBoxesByLastActiveTimeBetween(requesterId, startTime, endTime);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/messages")
    public ResponseEntity<Long> countMessages(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long requesterId = extractRequesterId(authentication);
        Long count = (startTime == null || endTime == null)
                ? adminService.countMessages(requesterId)
                : adminService.countMessagesByTimestampBetween(requesterId, startTime, endTime);
        return ResponseEntity.ok(count);
    }
}