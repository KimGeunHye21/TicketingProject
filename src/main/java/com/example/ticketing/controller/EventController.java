package com.example.ticketing.controller;


import com.example.ticketing.dto.event.*;
import com.example.ticketing.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    // 공연 리스트 조회
    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> getEvents(
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(eventService.getEvents(page));
    }

    // 공연 상세정보 조회
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEvent(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    // 공연 일자 정보 조회
    @GetMapping("/{eventId}/sessions")
    public ResponseEntity<List<EventSessionResponse>> getSessions(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(eventService.getSessions(eventId));
    }

    // 공연 구역 배치도 조회
    @GetMapping("/{eventId}/sections")
    public ResponseEntity<SectionMapResponse> getSections(
            @PathVariable Long eventId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(eventService.getSectionMap(eventId, sessionId));
    }

    // 공연 좌석 정보 조회
    @GetMapping("/{eventId}/sessions/{sessionId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long sectionId
    ) {
        return ResponseEntity.ok(
                eventService.getSeats(eventId, sessionId, sectionId)
        );
    }


}
