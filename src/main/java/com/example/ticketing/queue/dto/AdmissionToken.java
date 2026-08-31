package com.example.ticketing.queue.dto;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public final class AdmissionToken {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER =
            Base64.getUrlEncoder().withoutPadding();

    private final String value;
    private final Instant expiresAt;

    private AdmissionToken(
            String value,
            Instant expiresAt
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "입장 토큰 값은 비어 있을 수 없습니다."
            );
        }

        this.value = value;
        this.expiresAt = Objects.requireNonNull(
                expiresAt,
                "입장 토큰 만료시간은 필수입니다."
        );
    }

    /**
     * 새로운 입장 토큰 발급
     * 입장 토큰의 만료시간은 selectingExpiresAt을 초과하지 않음
     */
    public static AdmissionToken issue(
            Instant now,
            Instant selectingExpiresAt
    ) {
        Objects.requireNonNull(now, "현재 시간은 필수입니다.");
        Objects.requireNonNull(
                selectingExpiresAt,
                "좌석 선택 만료시간은 필수입니다."
        );

        if (!now.isBefore(selectingExpiresAt)) {
            throw new IllegalArgumentException(
                    "이미 만료된 좌석 선택 상태에는 입장 토큰을 발급할 수 없습니다."
            );
        }

        return new AdmissionToken(
                generateSecureValue(),
                selectingExpiresAt
        );
    }


    // 예측하기 어려운 랜덤 토큰 문자열을 만둠
    private static String generateSecureValue() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);

        return TOKEN_ENCODER.encodeToString(randomBytes);
    }


    public String value() {
        return value;
    }
    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean isExpiredAt(Instant now) {
        Objects.requireNonNull(now, "현재 시간은 필수입니다.");

        return !now.isBefore(expiresAt);
    }

    // 로그에 객체를 출력해도 실제 토큰값은 노출x
    @Override
    public String toString() {
        return "AdmissionToken{" +
                "value=[REDACTED]" +
                ", expiresAt=" + expiresAt +
                '}';
    }
}