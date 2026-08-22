package com.example.ticketing.dto.event;

import com.example.ticketing.domain.Event;
import com.example.ticketing.domain.EventSession;

import java.time.LocalDateTime;
import java.util.List;

public record EventDetailResponse(
        Long eventId,
        String title,
        String placeName,
        String address,
        Integer runningTime,
        String cast,
        Integer maxTicketPerUser,
        LocalDateTime bookingOpenAt,
        List<EventSessionResponse> sessions
) {

    public static EventDetailResponse from(
            Event event,
            List<EventSession> sessions,
            LocalDateTime now
    ) {
        List<EventSessionResponse> sessionResponses =
                sessions.stream()
                        .map(EventSessionResponse::from)
                        .toList();

        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getPlaceName(),
                event.getAddress(),
                event.getRunningTime(),
                event.getCast(),
                event.getMaxTicketPerUser(),
                event.getBookingOpenAt(),
                sessionResponses
        );
    }
}
