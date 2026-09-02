package com.example.ticketing.exception.queue;

// AdmissionTokenService 내부 재시도를 위한 예외
public class AdmissionTokenHashCollisionException extends RuntimeException {
    public AdmissionTokenHashCollisionException() {
        super("입장 토큰 해시가 충돌했습니다.");
    }
}
