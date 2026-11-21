package com.example.ticketing.controller;

import com.example.ticketing.dto.hold.SeatHoldResponse;
import com.example.ticketing.dto.hold.SeatStatusResponse;
import com.example.ticketing.exception.UnauthorizedException;
import com.example.ticketing.security.CustomUserDetails;
import com.example.ticketing.service.HoldService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class HoldController {
    private final HoldService holdService;

    // 좌석 선점
    @PostMapping("/seat-holds/{seatInstanceId}")
    public ResponseEntity<SeatHoldResponse> holdSeat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long seatInstanceId,
            HttpServletRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(holdService.holdSeat(userId, seatInstanceId, getCookies(request)));
    }

    // 구역 전체 좌석 상태 조회
    @GetMapping("/seat-holds/status/event/{eventId}/sessions/{sessionId}")
    public ResponseEntity<List<SeatStatusResponse>> getSeatStatuses(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long sectionId
    ) {
        List<SeatStatusResponse> response = holdService.getSeatStatuses(eventId, sessionId, sectionId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    private Map<String, String> getCookies(HttpServletRequest request) {
        Map<String, String> cookies = new HashMap<>();
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                cookies.putIfAbsent(cookie.getName(), cookie.getValue());
            }
        }
        return cookies;
    }
}
