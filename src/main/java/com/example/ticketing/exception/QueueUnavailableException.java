package com.example.ticketing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class QueueUnavailableException extends RuntimeException {

    public QueueUnavailableException(Throwable cause) {
        super(
                "대기열을 일시적으로 사용할 수 없습니다.",
                cause
        );
    }
}