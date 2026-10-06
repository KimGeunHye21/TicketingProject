package com.example.ticketing.controller;

import com.example.ticketing.queue.service.AdmissionTokenService;
import com.example.ticketing.security.AdmissionCookieFactory;
import com.example.ticketing.security.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.util.WebUtils;
import com.example.ticketing.dto.event.*;
import com.example.ticketing.security.CustomUserDetails;
import com.example.ticketing.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final AdmissionTokenService admissionTokenService;
    private final AdmissionCookieFactory admissionCookieFactory;

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


    // 공연 구역 배치도 조회
    @GetMapping("/events/{eventId}/sessions/{sessionId}/sections")
    public ResponseEntity<SectionMapResponse> getSections(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            HttpServletRequest request
    ) {

        verifyAdmission(request, userDetails.getUserId(), eventId, sessionId);

        return ResponseEntity.ok(eventService.getSectionMap(eventId, sessionId));
    }

    // 공연 좌석 정보 조회
    @GetMapping("/{eventId}/sessions/{sessionId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long sectionId,
            HttpServletRequest request
    ) {

        verifyAdmission(request, userDetails.getUserId(), eventId, sessionId);

        return ResponseEntity.ok(
                eventService.getSeats(eventId, sessionId, sectionId)
        );
    }


    private void verifyAdmission(
            HttpServletRequest request,
            Long userId,
            Long eventId,
            Long sessionId
    ) {
        String cookieName = admissionCookieFactory.cookieName(sessionId);
        Cookie admissionCookie = WebUtils.getCookie(request, cookieName);

        String rawToken = admissionCookie == null ? null : admissionCookie.getValue();

        admissionTokenService.verifyOrThrow(
                rawToken,
                userId,
                eventId,
                sessionId
        );
    }

}
