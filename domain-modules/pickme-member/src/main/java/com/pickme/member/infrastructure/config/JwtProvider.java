package com.pickme.member.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 30; // 30분
    private static final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7; // 7일

    private final SecretKey secretKey;

    public JwtProvider(@Value("${jwt.secret:pickme-default-jwt-secret-key-which-is-at-least-256-bits}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID memberId, String email) {
        return createToken(memberId, email, ACCESS_TOKEN_VALIDITY);
    }

    public String createRefreshToken(UUID memberId, String email) {
        return createToken(memberId, email, REFRESH_TOKEN_VALIDITY);
    }

    private String createToken(UUID memberId, String email, long validity) {
        Date now = new Date();
        return Jwts.builder()
                .subject(memberId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validity))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID getMemberId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }
}
