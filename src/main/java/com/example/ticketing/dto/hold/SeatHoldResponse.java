package com.example.ticketing.dto.hold;

import com.example.ticketing.hold.redis.SeatHoldRedisStore.HoldResult;

public record SeatHoldResponse(Long seatInstanceId, Long expiresAt) {

    public static SeatHoldResponse from(HoldResult result) {
        return new SeatHoldResponse(result.seatInstanceId(), result.expiresAt());
    }
}
