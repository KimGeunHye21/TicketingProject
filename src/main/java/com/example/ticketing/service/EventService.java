package com.example.ticketing.service;

import com.example.ticketing.domain.Event;
import com.example.ticketing.domain.EventSession;
import com.example.ticketing.domain.Section;
import com.example.ticketing.dto.event.EventDetailResponse;
import com.example.ticketing.dto.event.EventSessionResponse;
import com.example.ticketing.dto.event.EventSummaryResponse;
import com.example.ticketing.dto.event.SeatResponse;
import com.example.ticketing.dto.event.SectionMapResponse;
import com.example.ticketing.dto.event.SectionResponse;
import com.example.ticketing.exception.BookingNotOpenException;
import com.example.ticketing.exception.EventSessionNotFoundException;
import com.example.ticketing.repository.EventRepository;
import com.example.ticketing.repository.EventSessionRepository;
import com.example.ticketing.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private static final int EVENT_PAGE_SIZE = 10;

    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;
    private final SectionRepository sectionRepository;

    // 공연 목록 조회
    public Page<EventSummaryResponse> getEvents(int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }

        Pageable pageable = PageRequest.of(page, EVENT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));

        return eventRepository.findAll(pageable).map(event -> {
                    LocalDate startDate = eventSessionRepository.findFirstByEvent_IdOrderByStartAtAsc(event.getId()).map(session -> session.getStartAt().toLocalDate()).orElse(null);

                    LocalDate endDate = eventSessionRepository.findFirstByEvent_IdOrderByStartAtDesc(event.getId()).map(session -> session.getStartAt().toLocalDate()).orElse(null);

                    // 공연일이 하루뿐이면 종료일은 보내지 않음
                    if (startDate != null && startDate.equals(endDate)) {
                        endDate = null;
                    }

                    return EventSummaryResponse.from(event, startDate, endDate);
                });
    }

    // 공연 상세정보 조회
    public EventDetailResponse getEvent(Long eventId) {
        Event event = findEvent(eventId);

        List<EventSession> sessions = eventSessionRepository.findAllByEvent_IdOrderByStartAtAsc(eventId);

        return EventDetailResponse.from(event, sessions, LocalDateTime.now());
    }

    // 공연 구역 배치도 조회
    public SectionMapResponse getSectionMap(Long eventId, Long sessionId) {
        Event event = findEvent(eventId);
        EventSession session = eventSessionRepository.findByIdAndEvent_Id(sessionId, eventId)
                .orElseThrow(() -> new EventSessionNotFoundException(eventId, sessionId));
        LocalDateTime now = LocalDateTime.now();

        // 유효한 예매 시간인지
        if (now.isBefore(event.getBookingOpenAt())) {
            throw BookingNotOpenException.beforeOpen(event.getBookingOpenAt());
        }
        if (!now.isBefore(session.getStartAt())) {
            throw BookingNotOpenException.sessionStarted(session.getStartAt());
        }

        List<SectionResponse> sections = sectionRepository.findAllByEvent_IdOrderByIdAsc(eventId).
                stream().
                map(section -> SectionResponse.from(
                        section,
                        countAvailableSeats(sessionId, section.getId()))).
                toList();

        return new SectionMapResponse(event.getId(), event.getSeatMapViewBox(), sections);
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다."));
    }

    private long countAvailableSeats(Long sessionId, Long sectionId) {
        // TODO: 회차 선택 후 DB 상태와 Redis 선점 상태를 기준으로 계산
        return 0L;
    }
}
