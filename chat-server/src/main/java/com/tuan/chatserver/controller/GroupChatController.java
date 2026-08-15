package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.CursorPaginationRequest;
import com.tuan.chatserver.dto.CursorPaginationResponse;
import com.tuan.chatserver.dto.GroupChatDTO;
import com.tuan.chatserver.dto.RenameGroupChatRequest;
import com.tuan.chatserver.enums.GroupChatPermission;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.GroupChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/chats/gc")
public class GroupChatController {
    private final GroupChatService groupChatService;

    public GroupChatController(GroupChatService groupChatService){
        this.groupChatService = groupChatService;
    }

    @GetMapping("/{groupChatId}")
    public ResponseEntity<GroupChatDTO> getGroupChatById(Authentication authentication,
                                                         @PathVariable Long groupChatId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        GroupChatDTO dto = groupChatService.getGroupChatById(userId, groupChatId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{groupChatId}/permission")
    public ResponseEntity<GroupChatPermission> getGroupChatPermission(Authentication authentication, @PathVariable Long groupChatId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        GroupChatPermission permission = groupChatService.getGroupChatPermission(userId, groupChatId);
        return ResponseEntity.ok(permission);
    }

    @PostMapping
    public ResponseEntity<CursorPaginationResponse<List<GroupChatDTO>>> getAllGroupChat(Authentication authentication,
                                                                                        @RequestBody CursorPaginationRequest request){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        CursorPaginationResponse<List<GroupChatDTO>> dtos = groupChatService.getAllGroupChatByUserId(userId, request);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/search")
    public ResponseEntity<CursorPaginationResponse<List<GroupChatDTO>>> getGroupChatsByNameContaining(Authentication authentication,
                                                                                                      @RequestParam String keyword,
                                                                                                      @RequestBody CursorPaginationRequest request){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        CursorPaginationResponse<List<GroupChatDTO>> dtos = groupChatService.getGroupChatByNameContaining(keyword, userId, request);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/create")
    public ResponseEntity<GroupChatDTO> createGroupChat(Authentication authentication,
                                                        @RequestBody Set<Long> otherIds){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        GroupChatDTO dto = groupChatService.createGroupChat(userId, otherIds);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{groupChatId}")
    public ResponseEntity<Void> renameGroupChat(Authentication authentication,
                                                @Valid @RequestBody RenameGroupChatRequest request){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        groupChatService.renameGroupChat(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupChatId}/members")
    public ResponseEntity<Void> addMembersToGroup(Authentication authentication,
                                                  @PathVariable Long groupChatId,
                                                  @RequestBody Set<Long> otherIds){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        groupChatService.addMembersToGroup(userId, groupChatId, otherIds);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupChatId}/members/{memberId}")
    public ResponseEntity<Void> removeMemberFromGroup(Authentication authentication,
                                                      @PathVariable Long groupChatId,
                                                      @PathVariable Long memberId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        groupChatService.removeUserFromGroup(userId, groupChatId, memberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupChatId}/leave")
    public ResponseEntity<Void> outGroupChat(Authentication authentication,
                                             @PathVariable Long groupChatId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        groupChatService.outGroupChat(userId, groupChatId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupChatId}/members/{nomineeId}/promote/vice-leader")
    public ResponseEntity<Void> promoteToViceLeader(Authentication authentication,
                                                    @PathVariable Long groupChatId,
                                                    @PathVariable Long nomineeId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        groupChatService.promoteToViceLeader(userId, groupChatId, nomineeId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupChatId}/members/{nomineeId}/promote/leader")
    public ResponseEntity<Void> promoteToLeader(Authentication authentication,
                                                @PathVariable Long groupChatId,
                                                @PathVariable Long nomineeId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        groupChatService.promoteToLeader(userId, groupChatId, nomineeId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupChatId}/members/{nomineeId}/demote/vice-leader")
    public ResponseEntity<Void> demoteToViceLeader(Authentication authentication,
                                                   @PathVariable Long groupChatId,
                                                   @PathVariable Long nomineeId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        groupChatService.demoteToViceLeader(userId, groupChatId, nomineeId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupChatId}/members/{nomineeId}/demote/user")
    public ResponseEntity<Void> demoteToUser(Authentication authentication,
                                             @PathVariable Long groupChatId,
                                             @PathVariable Long nomineeId){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        groupChatService.demoteToUser(userId, groupChatId, nomineeId);
        return ResponseEntity.noContent().build();
    }
}