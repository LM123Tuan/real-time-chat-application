package com.tuan.chatserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuan.chatserver.dto.PendingRegistration;
import com.tuan.chatserver.exception.DataAccessFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class VerificationService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final String VERIFICATION_TOKEN_PREFIX = "verification:";
    private static final String VERIFICATION_EMAIL_PREFIX = "verification-email:";

    private static final String CREATE_VERIFICATION_SCRIPT =
            "local oldToken = redis.call('GET', KEYS[2]) " +
                    "if oldToken then " +
                    "    redis.call('DEL', ARGV[4] .. oldToken) " +
                    "end " +
                    "redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]) " +
                    "redis.call('SET', KEYS[2], ARGV[3], 'PX', ARGV[2]) " +
                    "return 1";

    private static final String REMOVE_VERIFICATION_SCRIPT =
            "redis.call('DEL', KEYS[1]) " +
                    "redis.call('DEL', KEYS[2]) " +
                    "return 1";

    private final DefaultRedisScript<Long> createVerificationScript;
    private final DefaultRedisScript<Long> removeVerificationScript;

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Autowired
    public VerificationService(RedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.createVerificationScript = new DefaultRedisScript<>(CREATE_VERIFICATION_SCRIPT, Long.class);
        this.removeVerificationScript = new DefaultRedisScript<>(REMOVE_VERIFICATION_SCRIPT, Long.class);
    }

    public boolean hasPendingVerification(String email) {
        logger.info("Checking pending verification for email={}", email);
        try {
            return redisService.exists(buildEmailKey(email));
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public void createVerification(String token, PendingRegistration pendingRegistration) {
        logger.info("Creating verification for email={}", pendingRegistration.getEmail());

        try {
            String tokenKey = buildTokenKey(token);
            String emailKey = buildEmailKey(pendingRegistration.getEmail());
            String pendingRegistrationJson = objectMapper.writeValueAsString(pendingRegistration);

            redisService.execute(
                    createVerificationScript,
                    List.of(tokenKey, emailKey),
                    pendingRegistrationJson,
                    String.valueOf(VERIFICATION_TTL.toMillis()),
                    token,
                    VERIFICATION_TOKEN_PREFIX
            );
        } catch (JsonProcessingException e) {
            throw new DataAccessFailureException(e);
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public Optional<PendingRegistration> getPendingRegistration(String token) {
        logger.info("Retrieving pending registration for token={}", token);
        try {
            Optional<String> json = redisService.getRaw(buildTokenKey(token));
            if (json.isEmpty()) {
                return Optional.empty();
            }
            PendingRegistration result = objectMapper.readValue(json.get(), PendingRegistration.class);
            return Optional.of(result);
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public void removeVerification(String token, String email) {
        logger.info("Removing verification for email={}", email);

        try {
            String tokenKey = buildTokenKey(token);
            String emailKey = buildEmailKey(email);

            redisService.execute(
                    removeVerificationScript,
                    List.of(tokenKey, emailKey)
            );
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public Optional<String> getTokenByEmail(String email) {
        logger.info("Retrieving verification token for email={}", email);
        try {
            Optional<String> token = redisService.getRaw(buildEmailKey(email));
            return token;
        } catch (Exception e) {
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