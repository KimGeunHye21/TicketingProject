package com.example.ticketing.oauth;

public record OAuthUserInfo(
        String providerId,
        String email,
        String name
) {
}
