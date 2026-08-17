package com.example.ticketing.dto.auth;

public record LoginResult(
        String accessToken,
        String refreshToken
) {
}