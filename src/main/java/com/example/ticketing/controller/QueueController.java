package com.example.ticketing.controller;

import com.example.ticketing.dto.queue.QueueJoinResponse;
import com.example.ticketing.security.CustomUserDetails;
import com.example.ticketing.service.QueueService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

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

}
