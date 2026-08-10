package com.example.ticketing.security;

import com.example.ticketing.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final long ACCESS_TOKEN_EXPIRATION =
            30 * 60 * 1000L; //30분
    private static final long REFRESH_TOKEN_EXPIRATION =
            14L * 24 * 60 * 60 * 1000; //14일
    private final SecretKey secretKey;


    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );
    }

    // Access Token 생성
    public String createAccessToken(User user) {

        return createToken(
                user,
                "access",
                ACCESS_TOKEN_EXPIRATION
        );
    }
    // Refresh Token 생성
    public String createRefreshToken(User user) {

        return createToken(
                user,
                "refresh",
                REFRESH_TOKEN_EXPIRATION
        );
    }

    // JWT에서 userId 꺼내기
    public Long getUserId(String token) {

        Claims claims = getClaims(token);

        return Long.valueOf(claims.getSubject());
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {

        try {
            getClaims(token);
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Access Token인지 확인
    public boolean isAccessToken(String token) {

        return "access".equals(
                getClaims(token).get("type", String.class)
        );
    }
    // Refresh Token인지 확인
    public boolean isRefreshToken(String token) {

        return "refresh".equals(
                getClaims(token).get("type", String.class)
        );
    }


    // 실제 JWT 생성
    private String createToken(
            User user,
            String type,
            long expirationTime
    ) {

        Date now = new Date();

        Date expiration =
                new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // JWT 해석
    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
