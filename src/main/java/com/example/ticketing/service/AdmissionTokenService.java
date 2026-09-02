package com.example.ticketing.service;

import com.example.ticketing.exception.AdmissionTokenHashCollisionException;
import com.example.ticketing.exception.QueueUnavailableException;
import com.example.ticketing.queue.AdmissionTokenRedisStore;
import com.example.ticketing.queue.domain.QueueStatus;
import com.example.ticketing.queue.domain.QueueTicket;
import com.example.ticketing.queue.dto.AdmissionToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AdmissionTokenService {

    private static final int MAX_ISSUE_ATTEMPTS = 3;
    private final AdmissionTokenRedisStore admissionTokenRedisStore;

    public Optional<AdmissionToken> issueIfAbsent(
            QueueTicket ticket,
            Instant selectingExpiresAt
    ) {
        validateTicket(
                ticket,
                selectingExpiresAt
        );

        // 해시가 충돌해도 최대 3번까지 해시 생성
        for (int attempt = 0; attempt < MAX_ISSUE_ATTEMPTS; attempt++) {

            AdmissionToken candidate =
                    AdmissionToken.issue(
                            Instant.now(),
                            selectingExpiresAt
                    );

            try {
                return admissionTokenRedisStore.saveIfAbsent(
                        ticket,
                        candidate
                );

            } catch (AdmissionTokenHashCollisionException exception) {

                if (attempt == MAX_ISSUE_ATTEMPTS -1) {
                    throw new QueueUnavailableException(exception);
                }
            }
        }

        throw new QueueUnavailableException(
                new IllegalStateException(
                        "입장 토큰 발급을 완료하지 못했습니다."
                )
        );
    }

    private void validateTicket(
            QueueTicket ticket,
            Instant selectingExpiresAt
    ) {
        Objects.requireNonNull(
                ticket,
                "QueueTicket은 필수입니다."
        );

        Objects.requireNonNull(
                selectingExpiresAt,
                "selectingExpiresAt은 필수입니다."
        );

        if (ticket.status() != QueueStatus.SELECTING) {
            throw new IllegalStateException(
                    "SELECTING 티켓에만 입장 토큰을 발급할 수 있습니다."
            );
        }

        Instant ticketSelectingExpiresAt = ticket.selectingExpiresAt();

        if (ticketSelectingExpiresAt == null) {
            throw new IllegalStateException(
                    "SELECTING 티켓에 selectingExpiresAt이 없습니다."
            );
        }

        // QueueStatusSnapshot에서 조회한 값과
        // QueueTicket에 저장된 값이 같은지 확인
        if (!ticketSelectingExpiresAt.equals(
                selectingExpiresAt
        )) {
            throw new IllegalStateException(
                    "티켓과 상태 조회 결과의 "
                            + "selectingExpiresAt이 일치하지 않습니다."
            );
        }
    }



}
