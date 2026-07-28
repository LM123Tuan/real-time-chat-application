package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.PendingRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class VerificationService {
    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final String VERIFICATION_TOKEN_PREFIX = "verification:";
    private static final String VERIFICATION_EMAIL_PREFIX = "verification-email:";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RedisService redisService;

    public VerificationService(RedisService redisService) {
        this.redisService = redisService;
    }

    public boolean hasPendingVerification(String email) {
        logger.info("Checking pending verification for email={}", email);
        return redisService.exists(buildEmailKey(email));
    }

    public void createVerification(String token, PendingRegistration pendingRegistration) {
        logger.info("Creating verification for email={}", pendingRegistration.getEmail());

        redisService.set(buildTokenKey(token), pendingRegistration, VERIFICATION_TTL);
        redisService.set(buildEmailKey(pendingRegistration.getEmail()), token, VERIFICATION_TTL);

        logger.info("Verification created successfully for email={}",
                pendingRegistration.getEmail());
    }

    public Optional<PendingRegistration> getPendingRegistration(String token) {
        logger.info("Retrieving pending registration for token={}", token);
        return redisService.get(buildTokenKey(token), PendingRegistration.class);
    }

    public void removeVerification(String token, String email) {
        logger.info("Removing verification for email={}", email);

        redisService.delete(buildTokenKey(token));
        redisService.delete(buildEmailKey(email));

        logger.info("Verification removed successfully for email={}", email);
    }

    public Optional<String> getTokenByEmail(String email) {
        logger.info("Retrieving verification token for email={}", email);
        return redisService.get(buildEmailKey(email), String.class);
    }

    private String buildTokenKey(String token) {
        return VERIFICATION_TOKEN_PREFIX + token;
    }

    private String buildEmailKey(String email) {
        return VERIFICATION_EMAIL_PREFIX + email;
    }
}