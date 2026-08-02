package com.tuan.chatserver.controller;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.service.AuthService;
import com.tuan.chatserver.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService,
                          UserService userService){
        this.authService=authService;
        this.userService=userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/oauth/exchange")
    public ResponseEntity<LoginResponse> exchangeToken(@RequestParam String exchangeToken){
        return ResponseEntity.ok(authService.exchangeToken(exchangeToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request){
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, @RequestBody LogoutRequest logoutRequest){
        authService.logout(authentication, logoutRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/initiate-reset-password")
    public ResponseEntity<Void> initiateResetPassword(@Valid @RequestBody ForgotPasswordRequest request){
        authService.initiateResetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/confirm-reset-password")
    public void confirmResetPassword(@RequestParam String token, HttpServletResponse response) throws IOException {
        String redirectUrl = authService.confirmResetPassword(token);
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestParam String token, @RequestBody ResetPasswordRequest request){
        authService.resetPassword(token, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyRegistration(@RequestParam String token){
        userService.verifyRegistration(token);
        return ResponseEntity.noContent().build();
    }
}
