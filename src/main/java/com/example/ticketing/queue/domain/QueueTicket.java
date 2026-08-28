package com.example.ticketing.queue.domain;

import java.time.Instant;

public record QueueTicket(
        String queueTicketId,
        Long userId,
        Long eventId,
        Long sessionId,
        long waitingNumber,
        QueueStatus status,
        Instant createdAt
) {

    public static QueueTicket waiting(
            String queueTicketId,
            Long userId,
            Long eventId,
            Long sessionId,
            Instant createdAt
    ) {
        return new QueueTicket(
                queueTicketId,
                userId,
                eventId,
                sessionId,
                0L, // 실제 대기 번호는 redis에서 발급
                QueueStatus.WAITING,
                createdAt
        );
    }
}
