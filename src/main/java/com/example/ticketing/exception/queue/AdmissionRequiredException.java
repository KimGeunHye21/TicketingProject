package com.example.ticketing.exception.queue;

public class AdmissionRequiredException extends RuntimeException {
    public AdmissionRequiredException(String message) {
        super("유효한 대기열 입장 권한이 필요합니다.");
    }
}
