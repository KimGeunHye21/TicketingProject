package com.example.ticketing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ResponseStatus(HttpStatus.CONFLICT)
public class BookingNotOpenException extends RuntimeException {

    private BookingNotOpenException(String message) {
        super(message);
    }

    public static BookingNotOpenException beforeOpen(
            LocalDateTime bookingOpenAt
    ) {
        return new BookingNotOpenException(
                "예매 시작 전입니다. bookingOpenAt="
                        + bookingOpenAt
        );
    }

    public static BookingNotOpenException sessionStarted(
            LocalDateTime startAt
    ) {
        return new BookingNotOpenException(
                "이미 시작된 공연 회차입니다. startAt="
                        + startAt
        );
    }
}