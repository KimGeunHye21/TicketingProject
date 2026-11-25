package com.example.ticketing.hold.cache;

import com.example.ticketing.domain.Event;
import com.example.ticketing.domain.EventSession;
import com.example.ticketing.exception.EventNotFoundException;
import com.example.ticketing.exception.EventSessionNotFoundException;
import com.example.ticketing.repository.EventRepository;
import com.example.ticketing.repository.EventSessionRepository;
import com.example.ticketing.repository.SeatInstanceRepository;
import com.example.ticketing.repository.SectionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SeatStatusQueryCache {

    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;
    private final SectionRepository sectionRepository;
    private final SeatInstanceRepository seatInstanceRepository;

    private final Cache<CacheKey, QueryData> cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    public QueryData get(Long eventId, Long sessionId, Long sectionId) {
        // cache.get(캐시가 존재하면 해당 데이터를 가져옴, 새로 sql에서 데이터를 load)
        return cache.get(new CacheKey(eventId, sessionId, sectionId), this::load);
    }

    private QueryData load(CacheKey key) {
        Long eventId = key.eventId();
        Long sessionId = key.sessionId();
        Long sectionId = key.sectionId();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        EventSession session = eventSessionRepository.findByIdAndEvent_Id(sessionId, eventId)
                .orElseThrow(() -> new EventSessionNotFoundException(eventId, sessionId));

        if (sectionId != null && !sectionRepository.existsByIdAndEvent_Id(sectionId, eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공연에 속한 구역을 찾을 수 없습니다.");
        }

        List<Long> seatInstanceIds = seatInstanceRepository.findIdsBySessionAndSection(sessionId, sectionId);

        return new QueryData(event.getBookingOpenAt(), session.getStartAt(), List.copyOf(seatInstanceIds));
    }

    private record CacheKey(Long eventId, Long sessionId, Long sectionId) {}

    public record QueryData(
            LocalDateTime bookingOpenAt,
            LocalDateTime sessionStartAt,
            List<Long> seatInstanceIds
    ) {}
}
