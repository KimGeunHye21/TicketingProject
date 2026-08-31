package com.example.ticketing.security;

import com.example.ticketing.queue.dto.AdmissionToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public class AdmissionCookieFactory {

    // 여러 회차에 동시에 입장할 수 있으므로
    // queue_admission_{sessionId} 형태로 사용
    private static final String COOKIE_NAME_PREFIX = "queue_admission_";
    private static final String COOKIE_PATH = "/";

    private final boolean secure;

    public AdmissionCookieFactory(
            @Value("${queue.admission-cookie.secure:true}")
            boolean secure
    ) {
        this.secure = secure;
    }

    public ResponseCookie create(
            Long sessionId,
            AdmissionToken admissionToken
    ) {
        validateSessionId(sessionId);

        Objects.requireNonNull(
                admissionToken,
                "admissionToken은 필수입니다."
        );

        Instant now = Instant.now();
        long maxAgeSeconds = calculateMaxAgeSeconds(
                now,
                admissionToken.expiresAt()
        );

        // 브라우저 쿠키 형태로 변환
        return ResponseCookie
                .from(
                        cookieName(sessionId),
                        admissionToken.value()
                )
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(
                        Duration.ofSeconds(maxAgeSeconds)
                )
                .build();
    }


    public ResponseCookie delete(Long sessionId) {
        validateSessionId(sessionId);

        return ResponseCookie
                .from(cookieName(sessionId), "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    public String cookieName(Long sessionId) {
        validateSessionId(sessionId);

        return COOKIE_NAME_PREFIX + sessionId;
    }

    private long calculateMaxAgeSeconds(
            Instant now,
            Instant expiresAt
    ) {
        Objects.requireNonNull(
                expiresAt,
                "입장 토큰 만료시간은 필수입니다."
        );

        if (!now.isBefore(expiresAt)) {
            return 0L;
        }

        Duration remaining = Duration.between(
                now,
                expiresAt
        );

        // 소수점 이하 초를 버립니다.
        return Math.max(
                0L,
                remaining.getSeconds()
        );
    }

    private void validateSessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0L) {
            throw new IllegalArgumentException(
                    "올바른 sessionId가 필요합니다."
            );
        }
    }
}
