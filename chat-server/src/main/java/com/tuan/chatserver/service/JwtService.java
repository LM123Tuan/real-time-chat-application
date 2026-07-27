package com.tuan.chatserver.service;

import com.tuan.chatserver.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    private SecretKey getSignKey(){
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String username, UserRole role, int tokenVersion){
        logger.debug("Generating access token for userId={}", userId);
        String token = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .claim("tokenVersion",tokenVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSignKey())
                .compact();
        logger.debug("Access token generated successfully for userId={}", userId);
        return token;
    }

    private Claims extractClaims(String token){
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public Integer extractTokenVersion(String token) {
        return extractClaims(token).get("tokenVersion", Integer.class);
    }

    private boolean isExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, String username) {
        boolean valid = username.equals(extractUsername(token)) && !isExpired(token);
        if (!valid) {
            logger.warn("Token validation failed for username={}", username);
        }
        return valid;
    }

    public String generateRefreshToken() {
        logger.debug("Generating refresh token");
        return UUID.randomUUID().toString();
    }
}