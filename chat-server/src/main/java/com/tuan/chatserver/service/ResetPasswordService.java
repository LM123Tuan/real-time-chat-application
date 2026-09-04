package com.tuan.chatserver.service;

import com.tuan.chatserver.exception.DataAccessFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class ResetPasswordService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Duration RESET_PASSWORD_TTL = Duration.ofMinutes(15);
    private static final String RESET_PASSWORD_TOKEN_PREFIX = "reset-password:";
    private static final String RESET_PASSWORD_EMAIL_PREFIX = "reset-password-email:";

    private static final String CREATE_RESET_PASSWORD_SCRIPT =
            "local oldToken = redis.call('GET', KEYS[2]) " +
                    "if oldToken then " +
                    "    redis.call('DEL', KEYS[2]) " +
                    "    redis.call('DEL', ARGV[3] .. oldToken) " +
                    "end " +
                    "redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]) " +
                    "redis.call('SET', KEYS[2], ARGV[4], 'PX', ARGV[2]) " +
                    "return 1";

    private static final String REMOVE_RESET_PASSWORD_SCRIPT =
            "redis.call('DEL', KEYS[1]) " +
                    "redis.call('DEL', KEYS[2]) " +
                    "return 1";

    private final DefaultRedisScript<Long> createResetPasswordScript;
    private final DefaultRedisScript<Long> removeResetPasswordScript;

    private final RedisService redisService;

    @Autowired
    public ResetPasswordService(RedisService redisService) {
        this.redisService=redisService;
        this.createResetPasswordScript = new DefaultRedisScript<>(CREATE_RESET_PASSWORD_SCRIPT, Long.class);
        this.removeResetPasswordScript = new DefaultRedisScript<>(REMOVE_RESET_PASSWORD_SCRIPT, Long.class);
    }

    public boolean hasPendingVerification(String email) {
        logger.info("Checking pending verification for email={}", email);
        try {
            return redisService.exists(buildEmailKey(email));
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public void createResetPassword(String token, String email) {
        logger.info("Creating reset password request for email={}", email);

        try {
            String tokenKey = buildTokenKey(token);
            String emailKey = buildEmailKey(email);

            redisService.execute(
                    createResetPasswordScript,
                    List.of(tokenKey, emailKey),
                    email,
                    String.valueOf(RESET_PASSWORD_TTL.toMillis()),
                    RESET_PASSWORD_TOKEN_PREFIX,
                    token
            );
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public Optional<String> getEmailByToken(String token) {
        logger.info("Retrieving email for reset password token={}", token);
        try {
            Object email = redisService.getRaw(buildTokenKey(token));
            return Optional.ofNullable(email).map(Object::toString);
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public void removeResetPassword(String token, String email) {
        logger.info("Removing reset password request for email={}", email);

        try {
            String tokenKey = buildTokenKey(token);
            String emailKey = buildEmailKey(email);

            redisService.execute(
                    removeResetPasswordScript,
                    List.of(tokenKey, emailKey)
            );
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public Optional<String> getTokenByEmail(String email) {
        logger.info("Retrieving reset password token for email={}", email);
        try {
            Object token = redisService.getRaw(buildEmailKey(email));
            return Optional.ofNullable(token).map(Object::toString);
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    private String buildTokenKey(String token) {
        return RESET_PASSWORD_TOKEN_PREFIX + token;
    }

    private String buildEmailKey(String email) {
        return RESET_PASSWORD_EMAIL_PREFIX + email;
    }
}