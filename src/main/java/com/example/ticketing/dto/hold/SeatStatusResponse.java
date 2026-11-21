package com.example.ticketing.dto.hold;

import com.example.ticketing.hold.redis.SeatHoldRedisStore.SeatStatusInfo;

public record SeatStatusResponse(
        Long seatInstanceId, String status, boolean isMine, Long expiresAt
) {

    public static SeatStatusResponse from(SeatStatusInfo seat) {
        return new SeatStatusResponse(
                seat.seatInstanceId(), seat.status().name(), seat.isMine(), seat.expiresAt()
        );
    }
}
