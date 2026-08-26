package com.example.ticketing.queue;

public enum QueueStatus {

    // 대기 중
    WAITING,

    // 입장 가능
    READY,

    // 대기열 통과 완료
    ENTERED,

    // 입장 가능 시간 만료
    EXPIRED,

    // 사용자 또는 시스템에 의해 취소
    CANCELLED;

    public boolean isActive() {
        return this == WAITING || this == READY;
    }
}