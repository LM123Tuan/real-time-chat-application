package com.tuan.chatserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class ResetPasswordService {
    private static final Duration RESET_PASSWORD_TTL = Duration.ofMinutes(15);
    private static final String RESET_PASSWORD_TOKEN_PREFIX = "reset-password:";
    private static final String RESET_PASSWORD_EMAIL_PREFIX = "reset-password-email:";

    private final RedisService redisService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ResetPasswordService(RedisService redisService){
        this.redisService=redisService;
    }

    private String buildTokenKey(String token) {
        return RESET_PASSWORD_TOKEN_PREFIX + token;
    }

    private String buildEmailKey(String email) {
        return RESET_PASSWORD_EMAIL_PREFIX + email;
    }

    public boolean hasPendingVerification(String email) {
        logger.info("Checking pending verification for email={}", email);
        return redisService.exists(buildEmailKey(email));
    }

    public void createResetPassword(String token, String email) {
        logger.info("Creating reset password request for email={}", email);

        redisService.set(buildTokenKey(token), email, RESET_PASSWORD_TTL);
        redisService.set(buildEmailKey(email), token, RESET_PASSWORD_TTL);

        logger.info("Reset password request created successfully for email={}", email);
    }

    public Optional<String> getEmailByToken(String token) {
        logger.info("Retrieving email for reset password token={}", token);
        return redisService.get(buildTokenKey(token), String.class);
    }

    public void removeResetPassword(String token, String email) {
        logger.info("Removing reset password request for email={}", email);

        redisService.delete(buildTokenKey(token));
        redisService.delete(buildEmailKey(email));

        logger.info("Reset password request removed successfully for email={}", email);
    }

    public Optional<String> getTokenByEmail(String email) {
        logger.info("Retrieving reset password token for email={}", email);
        return redisService.get(buildEmailKey(email), String.class);
    }
}