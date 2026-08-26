package com.example.ticketing.queue.dto;

import java.time.Instant;

public record AdmissionToken(
        String value,
        Instant expiresAt
) {
}
