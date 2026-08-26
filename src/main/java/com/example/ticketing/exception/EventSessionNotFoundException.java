package com.example.ticketing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EventSessionNotFoundException extends RuntimeException {

    public EventSessionNotFoundException(
            Long eventId,
            Long sessionId
    ) {
        super(
                "공연 회차를 찾을 수 없습니다. eventId="
                        + eventId
                        + ", sessionId="
                        + sessionId
        );
    }
}