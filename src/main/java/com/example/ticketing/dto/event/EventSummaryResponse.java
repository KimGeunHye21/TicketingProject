package com.example.ticketing.dto.event;

import com.example.ticketing.domain.Event;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long eventId,
        String title,
        String cast,
        LocalDateTime bookingOpenAt,
        LocalDate startDate,
        LocalDate endDate
) {

    public static EventSummaryResponse from(
            Event event,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getCast(),
                event.getBookingOpenAt(),
                startDate,
                endDate
        );
    }
}
