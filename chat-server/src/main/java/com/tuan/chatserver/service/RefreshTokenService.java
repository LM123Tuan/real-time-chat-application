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
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService){
        this.refreshTokenRepository=refreshTokenRepository;
        this.jwtService=jwtService;
    }

    @Transactional
    public RefreshToken createRefreshToken(Person person){
        logger.info("Creating refresh token for personId={}", person.getId());

        refreshTokenRepository.deleteByPerson(person);
        String token = jwtService.generateRefreshToken();
        LocalDateTime expiryDate = LocalDateTime.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS);
        RefreshToken refreshToken = new RefreshToken(token, expiryDate, person);

        try{
            RefreshToken saved = refreshTokenRepository.save(refreshToken);
            logger.info("Refresh token created successfully for personId={}", person.getId());
            return saved;
        }catch(Exception e){
            logger.error("Failed to save refresh token for personId={}", person.getId(), e);
            throw new DataAccessFailureException(e);
        }
    }

    public Optional<RefreshToken> findByToken(String token){
        logger.debug("Looking up refresh token");
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token){
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Refresh token expired for personId={}", token.getPerson().getId());
            refreshTokenRepository.delete(token);
            throw new RefreshTokenExpiredException(token.getPerson().getId());
        }
        return token;
    }

    public void logout(Person person){
        refreshTokenRepository.deleteByPerson(person);
    }
}