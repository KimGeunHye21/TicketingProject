package com.example.ticketing.dto.user;

import com.example.ticketing.domain.Role;

public record UserResponse(
        Long id,
        String email,
        String name,
        Role role

) {
}
