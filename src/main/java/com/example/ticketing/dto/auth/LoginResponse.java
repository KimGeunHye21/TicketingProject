package com.example.ticketing.dto.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
