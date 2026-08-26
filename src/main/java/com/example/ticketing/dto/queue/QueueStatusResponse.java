package com.example.ticketing.dto.queue;

import com.example.ticketing.queue.domain.QueueStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueStatusResponse(

        QueueStatus status,

        // WAITING일 때만 반환
        Long aheadCount,

        // SELECTING일 때만 반환
        Instant selectingExpiresAt,

        // WAITING일 때 다음 조회 권장 시간
        Long nextPollAfterMs
) {


    public static QueueStatusResponse waiting(
            long aheadCount,
            long nextPollAfterMs
    ) {
        return new QueueStatusResponse(
                QueueStatus.WAITING,
                aheadCount,
                null,
                nextPollAfterMs
        );
    }

    public static QueueStatusResponse selecting(
            Instant selectingExpiresAt
    ) {
        return new QueueStatusResponse(
                QueueStatus.SELECTING,
                null,
                selectingExpiresAt,
                null
        );
    }

    public static QueueStatusResponse checkout() {
        return new QueueStatusResponse(
                QueueStatus.CHECKOUT,
                null,
                null,
                null
        );
    }

    public static QueueStatusResponse terminal(
            QueueStatus status
    ) {
        return new QueueStatusResponse(
                status,
                null,
                null,
                null
        );
    }
}