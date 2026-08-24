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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                EVENT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return eventRepository.findAll(pageable)
                .map(EventSummaryResponse::from);
    }

    // 공연 상세정보 조회
    public EventDetailResponse getEvent(Long eventId) {
        Event event = findEvent(eventId);

        List<EventSession> sessions =
                eventSessionRepository
                        .findAllByEvent_IdOrderByStartAtAsc(eventId);

        return EventDetailResponse.from(
                event,
                sessions,
                LocalDateTime.now()
        );
    }

    // 공연 회차 조회
    public List<EventSessionResponse> getSessions(Long eventId) {
        findEvent(eventId);

        return eventSessionRepository
                .findAllByEvent_IdOrderByStartAtAsc(eventId)
                .stream()
                .map(EventSessionResponse::from)
                .toList();
    }

    // 공연 구역 배치도 조회
    public SectionMapResponse getSectionMap(Long eventId, Long sessionId) {
        Event event = findEvent(eventId);

        List<SectionResponse> sections =
                sectionRepository
                        .findAllByEvent_IdOrderByIdAsc(eventId)
                        .stream()
                        .map(section -> SectionResponse.from(
                                section,
                                countAvailableSeats(
                                        sessionId,
                                        section.getId()
                                )
                        ))
                        .toList();

        return new SectionMapResponse(
                event.getId(),
                event.getSeatMapViewBox(),
                sections
        );
    }

    // 공연 좌석 정보 조회
    public List<SeatResponse> getSeats(
            Long eventId,
            Long sessionId,
            Long sectionId
    ) {
        // TODO: sessionId가 eventId에 속한 회차인지 검증
        // TODO: sectionId가 eventId에 속한 구역인지 검증
        // TODO: DB의 RESERVED 상태와 Redis의 임시 선점 상태를 함께 조회
        // TODO: 폴링, SSE 또는 WebSocket 중 실시간 갱신 방식을 결정
        // TODO: sectionId가 null이면 전체 좌석, 값이 있으면 해당 구역만 조회
        // TODO: Seat와 Section을 fetch join해 N+1 조회 문제 방지

        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "실시간 좌석 조회 기능은 아직 구현되지 않았습니다."
        );
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "공연을 찾을 수 없습니다."
                ));
    }

    private long countAvailableSeats(Long sessionId, Long sectionId) {
        // TODO: 회차 선택 후 DB 상태와 Redis 선점 상태를 기준으로 계산
        return 0L;
    }
}
