package com.example.ticketing.dto.queue;

import com.example.ticketing.queue.QueueStatus;
import com.example.ticketing.queue.QueueTicket;

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
