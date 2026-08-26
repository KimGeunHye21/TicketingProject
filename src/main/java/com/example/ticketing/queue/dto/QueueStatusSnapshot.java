package com.example.ticketing.queue.dto;

import com.example.ticketing.queue.domain.QueueStatus;

import java.time.Instant;

public record QueueStatusSnapshot(

        QueueStatus status,

        // Redis ZRANK. WAITING이 아니면 null
        Long aheadCount,

        Instant selectingExpiresAt
) {
}
