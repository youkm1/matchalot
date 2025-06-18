package com.smwu.matchalot.web.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long validityInMilliseconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;

        log.info("JWT token 생성 with expriation time: {} ms ", validityInMilliseconds);
    }

    public String createToken(String userId, String email, String nickname) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        String token = Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("nickname", nickname)
                .claim("type","access_token")
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();

        log.debug("Token created: {} \n for user: {}", token, userId);
        return token;
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }
    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }


    public String getNickname(String token) {
        return parseClaims(token).get("nickname", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);

            // 토큰 타입 확인
            String tokenType = claims.get("type", String.class);
            if (!"access_token".equals(tokenType)) {
                log.warn("Invalid token type: {}", tokenType);
                return false;
            }

            // 만료 시간 확인 (자동으로 체크됨)
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void logTokenInfo(String token) {
        try {
            Claims claims = parseClaims(token);
            log.info("Token Info - Subject: {}, Email: {}, Issued: {}, Expires: {}",
                    claims.getSubject(),
                    claims.get("email"),
                    claims.getIssuedAt(),
                    claims.getExpiration());
        } catch (Exception e) {
            log.error("Failed to parse token for logging: {}", e.getMessage());
        }
    }


}
