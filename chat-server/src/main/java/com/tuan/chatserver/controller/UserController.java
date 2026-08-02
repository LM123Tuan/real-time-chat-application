package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.security.CustomUserDetails;
import com.tuan.chatserver.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService=userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody UserRegisterRequest request){
        userService.register(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication authentication, @NotBlank @RequestBody String password){
        userService.deleteAccount(authentication, password);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MyProfileDTO> getMyProfile(Authentication authentication) {
        Long id = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        return ResponseEntity.ok(userService.getProfile(id));
    }

    @PostMapping("/me")
    public ResponseEntity<Void> updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request){
        Long id = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        userService.updateProfile(id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request){
        Long id = ((CustomUserDetails) authentication.getPrincipal()).getPerson().getId();
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<OtherProfileDTO> findUser(@RequestParam String identifier){
        OtherProfileDTO user= userService.findActiveUserByUsernameOrEmail(identifier);
        return ResponseEntity.ok(user);
    }
}
