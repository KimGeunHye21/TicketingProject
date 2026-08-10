package com.example.ticketing.security;

import lombok.Getter;

@Getter
public class CustomUserDetails {

    private final Long userId;

    public CustomUserDetails(Long userId) {
        this.userId = userId;
    }

}
