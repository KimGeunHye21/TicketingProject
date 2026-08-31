package com.example.ticketing.controller;

import com.example.ticketing.dto.queue.QueueJoinResponse;
import com.example.ticketing.dto.queue.QueueStatusResponse;
import com.example.ticketing.queue.dto.QueueStatusResult;
import com.example.ticketing.security.AdmissionCookieFactory;
import com.example.ticketing.security.CustomUserDetails;
import com.example.ticketing.service.QueueService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final AdmissionCookieFactory admissionCookieFactory;


    // 대기열 참가
    @PostMapping("/events/{eventId}/sessions/{sessionId}")
    public ResponseEntity<QueueJoinResponse> joinQueue(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,
            @PathVariable
            Long eventId,
            @PathVariable
            Long sessionId
    ) {

        Long userId = userDetails.getUserId();

        QueueJoinResponse response =
                queueService.joinQueue(
                        userId,
                        eventId,
                        sessionId
                );

        return ResponseEntity.ok(response);
    }

    // 대기열 상태 조회
    @GetMapping("/events/{eventId}/sessions/{sessionId}")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId,
            @PathVariable Long sessionId
    ) {
        Long userId = userDetails.getUserId();

        QueueStatusResult result = queueService.getQueueStatus(
                userId,
                eventId,
                sessionId
        );

        ResponseEntity.BodyBuilder responseBuilder =
                ResponseEntity.ok();

        // 입장 토큰은 HttpOnly 쿠키의 Set-Cookie 헤더로만 전달
        if (result.hasAdmissionToken()) {
            ResponseCookie admissionCookie =
                    admissionCookieFactory.create(
                            sessionId,
                            result.getAdmissionToken()
                    );

            responseBuilder.header(
                    HttpHeaders.SET_COOKIE,
                    admissionCookie.toString()
            );
        }

        // JSON에는 QueueStatusResponse만 포함
        return responseBuilder.body(result.getResponse());
    }


}
