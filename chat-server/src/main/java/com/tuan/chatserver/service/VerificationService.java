package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.PendingRegistration;
import com.tuan.chatserver.exception.DataAccessFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class VerificationService {
    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final String VERIFICATION_TOKEN_PREFIX = "verification:";
    private static final String VERIFICATION_EMAIL_PREFIX = "verification-email:";

    private static final Logger logger =
            LoggerFactory.getLogger(VerificationService.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public VerificationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean hasPendingVerification(String email) {
        logger.info("Checking pending verification for email={}", email);
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(buildEmailKey(email))
            );
        } catch (DataAccessException e) {
            logger.error("Failed to check pending verification for email={}", email, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void createVerification(String token, PendingRegistration pendingRegistration) {
        logger.info("Creating verification for email={}", pendingRegistration.getEmail());
        try {
            redisTemplate.opsForValue().set(
                    buildTokenKey(token),
                    pendingRegistration,
                    VERIFICATION_TTL
            );
            redisTemplate.opsForValue().set(
                    buildEmailKey(pendingRegistration.getEmail()),
                    token,
                    VERIFICATION_TTL
            );
        } catch (DataAccessException e) {
            logger.error("Failed to create verification for email={}",
                    pendingRegistration.getEmail(), e);
            throw new DataAccessFailureException(e);
        }
        logger.info("Verification created successfully for email={}",
                pendingRegistration.getEmail());
    }

    public Optional<PendingRegistration> getPendingRegistration(String token) {
        logger.info("Retrieving pending registration for token={}", token);
        try {
            Object value = redisTemplate.opsForValue().get(buildTokenKey(token));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of((PendingRegistration) value);
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve pending registration for token={}", token, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void removeVerification(String token, String email) {
        logger.info("Removing verification for email={}", email);
        try {
            redisTemplate.delete(buildTokenKey(token));
            redisTemplate.delete(buildEmailKey(email));
        } catch (DataAccessException e) {
            logger.error("Failed to remove verification for email={}", email, e);
            throw new DataAccessFailureException(e);
        }
        logger.info("Verification removed successfully for email={}", email);
    }

    public Optional<String> getTokenByEmail(String email) {
        logger.info("Retrieving verification token for email={}", email);
        try {
            Object value = redisTemplate.opsForValue().get(buildEmailKey(email));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of((String) value);
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve verification token for email={}", email, e);
            throw new DataAccessFailureException(e);
        }
    }

    private String buildTokenKey(String token) {
        return VERIFICATION_TOKEN_PREFIX + token;
    }

    private String buildEmailKey(String email) {
        return VERIFICATION_EMAIL_PREFIX + email;
    }
}