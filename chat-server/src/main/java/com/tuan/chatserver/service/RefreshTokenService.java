package com.tuan.chatserver.service;

import com.tuan.chatserver.entity.Person;
import com.tuan.chatserver.entity.RefreshToken;
import com.tuan.chatserver.exception.DataAccessFailureException;
import com.tuan.chatserver.exception.RefreshTokenExpiredException;
import com.tuan.chatserver.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService, RedisTemplate<String, Object> redisTemplate){
        this.refreshTokenRepository=refreshTokenRepository;
        this.jwtService=jwtService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public RefreshToken createRefreshToken(Person person){
        logger.info("Creating refresh token for personId={}", person.getId());

        String token = jwtService.generateRefreshToken();
        LocalDateTime expiryDate = LocalDateTime.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS);
        RefreshToken refreshToken = new RefreshToken(token, expiryDate, person);

        try {
            RefreshToken saved = refreshTokenRepository.save(refreshToken);

            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + token,
                    "1",
                    Duration.ofMillis(refreshTokenExpiration)
            );

            logger.info("Refresh token created successfully for personId={}", person.getId());
            return saved;
        } catch (Exception e) {
            logger.error("Failed to create refresh token for personId={}", person.getId(), e);
            throw new DataAccessFailureException(e);
        }
    }

    public Optional<RefreshToken> findByToken(String token){
        logger.debug("Looking up refresh token");
        return refreshTokenRepository.findByToken(token);
    }

    public void validateToken(RefreshToken refreshToken) {
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + refreshToken.getToken()))) {
                throw new RefreshTokenExpiredException(refreshToken.getPerson().getId());
            }
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public void revoke(String refreshToken) {
        try{
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);
        }catch (Exception e){
            throw new DataAccessFailureException(e);
        }
    }
}