package com.example.ticketing.dto.event;

import com.example.ticketing.domain.Event;

import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long eventId,
        String title,
        String cast,
        LocalDateTime bookingOpenAt
) {

    public static EventSummaryResponse from(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getCast(),
                event.getBookingOpenAt()
        );
    }
}
