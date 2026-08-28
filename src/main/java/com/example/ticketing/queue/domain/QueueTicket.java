package com.example.ticketing.queue.domain;

import java.time.Instant;
import java.util.Objects;

public record QueueTicket(
        String queueTicketId,
        Long userId,
        Long eventId,
        Long sessionId,
        long waitingNumber,
        QueueStatus status,
        Instant createdAt,
        //Instant lastSeenAt,
        Instant selectingStartedAt,
        Instant selectingExpiresAt
) {

    public QueueTicket {
        requireNonNull(queueTicketId, "queueTicketId");
        requireNonNull(userId, "userId");
        requireNonNull(eventId, "eventId");
        requireNonNull(sessionId, "sessionId");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");


        if (waitingNumber < 0L) {
            throw new IllegalArgumentException(
                    "waitingNumber는 0 이상이어야 합니다."
            );
        }

        // WAITING에는 SELECTING 관련 시간이 없어야 함
        if (status == QueueStatus.WAITING
                && (selectingStartedAt != null
                || selectingExpiresAt != null)) {
            throw new IllegalArgumentException(
                    "WAITING 상태에는 SELECTING 관련 시간이 존재할 수 없습니다."
            );
        }

        // SELECTING에는 시작 시각과 만료 시각이 모두 필요
        if (status == QueueStatus.SELECTING
                && (selectingStartedAt == null
                || selectingExpiresAt == null)) {
            throw new IllegalArgumentException(
                    "SELECTING 상태에는 selectingStartedAt과 "
                            + "selectingExpiresAt이 필요합니다."
            );
        }

        if (selectingStartedAt != null
                && selectingExpiresAt != null
                && !selectingExpiresAt.isAfter(
                selectingStartedAt
        )) {
            throw new IllegalArgumentException(
                    "selectingExpiresAt은 selectingStartedAt보다 "
                            + "이후여야 합니다."
            );
        }
    }

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
                0L,
                QueueStatus.WAITING,
                createdAt,
                null,
                null
        );
    }

    private static void requireNonNull(Object value, String name) {
        Objects.requireNonNull(value, name + "는 필수입니다.");
    }
}
