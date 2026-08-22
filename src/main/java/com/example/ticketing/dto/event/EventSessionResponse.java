package com.example.ticketing.dto.event;

import com.example.ticketing.domain.EventSession;

import java.time.LocalDateTime;

public record EventSessionResponse(
        Long sessionId,
        LocalDateTime startAt
) {

    public static EventSessionResponse from(EventSession session) {
        return new EventSessionResponse(
                session.getId(),
                session.getStartAt()
        );
    }
}
