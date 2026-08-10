package com.example.ticketing.oauth;

import com.example.ticketing.domain.Provider;

public record OAuthUserInfo(
        Provider provider,
        String providerId,
        String email,
        String name
) {
}
