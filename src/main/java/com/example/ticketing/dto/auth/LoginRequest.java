package com.example.ticketing.dto.auth;

public record LoginRequest(
        String authorizationCode,
        String codeVerifier
) {
}
