package com.example.ticketing.queue.domain;

public enum QueueStatus {

    // 대기 중
    WAITING,

    // 대기열 통과
    SELECTING,

    // 결제중
    CHECKOUT,

    // 입장 가능 시간 만료
    EXPIRED,

    // 사용자 또는 시스템에 의해 취소
    CANCELLED;

    public boolean isActive() {
        return this == WAITING || this == SELECTING || this == CHECKOUT;
    }

    public boolean requiresWaitingHeartbeat() {
        return this == WAITING;
    }

    public boolean isTerminal() {
        return this == EXPIRED
                || this == CANCELLED;
    }
}