package com.example.ticketing.queue.dto;

import com.example.ticketing.queue.domain.QueueStatus;
import com.example.ticketing.queue.domain.QueueTicket;

import java.util.Objects;

public record QueueTicketQueryResult(
        QueueTicket ticket,

        // WAITING: Redis ZRANK
        // 다른 상태: null
        Long aheadCount
) {
    public QueueTicketQueryResult {
        Objects.requireNonNull(
                ticket,
                "QueueTicket은 필수입니다."
        );

        if (ticket.status() == QueueStatus.WAITING && aheadCount == null) {
            throw new IllegalArgumentException(
                    "WAITING 티켓에는 aheadCount가 필요합니다."
            );
        }

        if (ticket.status() != QueueStatus.WAITING && aheadCount != null) {
            throw new IllegalArgumentException(
                    "WAITING이 아닌 티켓에는 aheadCount가 없어야 합니다."
            );
        }

        if (aheadCount != null && aheadCount < 0L) {
            throw new IllegalArgumentException(
                    "aheadCount는 음수일 수 없습니다."
            );
        }
    }
}
