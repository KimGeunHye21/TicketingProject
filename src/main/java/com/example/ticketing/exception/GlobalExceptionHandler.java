package com.example.ticketing.exception;

import com.example.ticketing.exception.queue.AdmissionRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 모든 Controller에서 발생하는 예외를 공통 처리
@RestControllerAdvice
public class GlobalExceptionHandler {

    // UnauthorizedException 발생 시 이 메서드 실행
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorized(
            UnauthorizedException exception
    ) {
        // HTTP 상태 코드는 401,
        // body에는 예외 메시지를 담아서 반환
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exception.getMessage());
    }

    // AdmissionRequiredException 예외 발생 -> 401이 아닌 403으로 반환
    @ExceptionHandler(AdmissionRequiredException.class)
    public ResponseEntity<String> handleAdmissionRequired(
            AdmissionRequiredException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(exception.getMessage());
    }
}
