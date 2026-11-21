package com.example.ticketing.seathold.redis;

public final class SeatHoldRedisKey {
    private SeatHoldRedisKey() {
    }

    public static String reservations(Long sessionId) {
        return prefix(sessionId) + "reservations";
    }

    public static String hold(Long sessionId, Long seatInstanceId) {
        return prefix(sessionId) + "hold:" + seatInstanceId;
    }

    public static String userHolds(Long sessionId, Long userId) {
        return prefix(sessionId) + "user:" + userId + ":holds";
    }

    private static String prefix(Long sessionId) {
        return "seat:{" + sessionId + "}:";
    }
}