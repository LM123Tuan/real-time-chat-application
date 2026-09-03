package com.tuan.chatserver.service;

import com.tuan.chatserver.entity.Person;
import com.tuan.chatserver.entity.RefreshToken;
import com.tuan.chatserver.exception.DataAccessFailureException;
import com.tuan.chatserver.exception.RefreshTokenExpiredOrNotExistsException;
import com.tuan.chatserver.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String PERSON_PREFIX = "person:";

    private static final String CREATE_REFRESH_TOKEN_SCRIPT =
            "local oldToken = redis.call('GET', KEYS[1]) " +
                    "if oldToken then " +
                    "  redis.call('DEL', KEYS[1]) " +
                    "  redis.call('DEL', ARGV[3] .. oldToken) " +
                    "end " +
                    "redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]) " +
                    "redis.call('SET', KEYS[2], ARGV[4], 'PX', ARGV[2]) " +
                    "return 1";

    private static final String REVOKE_SCRIPT =
            "local personId = redis.call('GET', KEYS[1]) " +
                    "redis.call('DEL', KEYS[1]) " +
                    "if personId then " +
                    "  redis.call('DEL', ARGV[1] .. personId) " +
                    "end " +
                    "return 1";

    private final DefaultRedisScript<Long> createRefreshTokenScript;
    private final DefaultRedisScript<Long> revokeScript;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RedisService redisService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtService jwtService,
                               RedisService redisService){
        this.refreshTokenRepository=refreshTokenRepository;
        this.jwtService=jwtService;
        this.redisService = redisService;
        this.createRefreshTokenScript = new DefaultRedisScript<>(CREATE_REFRESH_TOKEN_SCRIPT, Long.class);
        this.revokeScript = new DefaultRedisScript<>(REVOKE_SCRIPT, Long.class);
    }

    @Transactional
    public RefreshToken createRefreshToken(Person person){
        logger.info("Creating refresh token for personId={}", person.getId());

        String personKey = PERSON_PREFIX + person.getId();
        String token = jwtService.generateRefreshToken();
        String tokenKey = REFRESH_TOKEN_PREFIX + token;

        try {
            LocalDateTime expiryDate = LocalDateTime.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS);
            RefreshToken refreshToken = new RefreshToken(token, expiryDate, person);
            RefreshToken saved = refreshTokenRepository.save(refreshToken);

            redisService.execute(
                    createRefreshTokenScript,
                    List.of(personKey, tokenKey),
                    token,
                    String.valueOf(refreshTokenExpiration),
                    REFRESH_TOKEN_PREFIX,
                    person.getId()
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

    public Optional<String> findTokenByPersonId(Long personId) {
        logger.debug("Looking up refresh token for personId={}", personId);
        try {
            Object token = redisService.get(PERSON_PREFIX + personId, String.class);
            return Optional.ofNullable(token).map(Object::toString);
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public void validateToken(RefreshToken refreshToken) {
        try {
            if (!redisService.exists(REFRESH_TOKEN_PREFIX + refreshToken.getToken())) {
                throw new RefreshTokenExpiredOrNotExistsException(refreshToken.getPerson().getId());
            }
        } catch (RefreshTokenExpiredOrNotExistsException e) {
            throw e;
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }

    public void revoke(String refreshToken) {
        try {
            String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;

            redisService.execute(
                    revokeScript,
                    List.of(tokenKey),
                    PERSON_PREFIX
            );
        } catch (Exception e) {
            throw new DataAccessFailureException(e);
        }
    }
}