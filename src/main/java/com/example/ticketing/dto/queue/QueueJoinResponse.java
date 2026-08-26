package com.example.ticketing.dto.queue;

import com.example.ticketing.queue.domain.QueueStatus;
import com.example.ticketing.queue.domain.QueueTicket;

public record QueueJoinResponse(
        long waitingNumber,
        QueueStatus status
) {
    public static QueueJoinResponse from(QueueTicket ticket) {
        return new QueueJoinResponse(
                ticket.waitingNumber(),
                ticket.status()
        );
    }
}
