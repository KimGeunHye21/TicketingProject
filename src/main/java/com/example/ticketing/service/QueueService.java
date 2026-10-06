package com.example.ticketing.service;

import com.example.ticketing.domain.Event;
import com.example.ticketing.domain.EventSession;
import com.example.ticketing.dto.queue.QueueJoinResponse;
import com.example.ticketing.dto.queue.QueueStatusResponse;
import com.example.ticketing.exception.*;
import com.example.ticketing.exception.queue.QueueNotFoundException;
import com.example.ticketing.exception.queue.QueueUnavailableException;
import com.example.ticketing.queue.dto.QueueTicketQueryResult;
import com.example.ticketing.queue.redis.QueueHeartbeatRedisStore;
import com.example.ticketing.queue.redis.QueueRedisStore;
import com.example.ticketing.queue.domain.QueueStatus;
import com.example.ticketing.queue.domain.QueueTicket;
import com.example.ticketing.queue.dto.AdmissionToken;
import com.example.ticketing.queue.dto.QueueStatusResult;
import com.example.ticketing.queue.service.AdmissionTokenService;
import com.example.ticketing.repository.EventRepository;
import com.example.ticketing.repository.EventSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueService {

    private static final Duration QUEUE_GRACE_PERIOD = Duration.ofHours(1);

    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;
    private final QueueRedisStore queueRedisStore;
    private final AdmissionTokenService admissionTokenService;
    private final QueueHeartbeatRedisStore queueHeartbeatRedisStore;

    // 대기열 등록
    public QueueJoinResponse joinQueue(
            Long userId,
            Long eventId,
            Long sessionId
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        EventSession session = eventSessionRepository
                .findByIdAndEvent_Id(sessionId, eventId)
                .orElseThrow(() -> new EventSessionNotFoundException(
                        eventId,
                        sessionId
                ));

        // 유효한 예매 시간 이내인지
        LocalDateTime bookingNow = LocalDateTime.now();
        validateBookingTime(event, session, bookingNow);


        // 새로운 대기열 티켓 생성
        Instant queueNow = Instant.now();
        return registerQueueTicket(
                userId,
                eventId,
                session,
                bookingNow,
                queueNow
        );
    }

    private void validateBookingTime(
            Event event,
            EventSession session,
            LocalDateTime now
    ) {
        if (now.isBefore(event.getBookingOpenAt())) {
            throw BookingNotOpenException.beforeOpen(
                    event.getBookingOpenAt()
            );
        }

        if (!now.isBefore(session.getStartAt())) {
            throw BookingNotOpenException.sessionStarted(
                    session.getStartAt()
            );
        }
    }

    private QueueJoinResponse registerQueueTicket(
            Long userId,
            Long eventId,
            EventSession session,
            LocalDateTime bookingNow,
            Instant queueNow
    ) {
        QueueTicket candidate = QueueTicket.waiting(
                UUID.randomUUID().toString(), // 예측 불가능한 queueTicketId 발급
                userId,
                eventId,
                session.getId(),
                queueNow
        );

        Duration expiration = Duration.between(
                bookingNow,
                session.getStartAt()
        ).plus(QUEUE_GRACE_PERIOD);

        // Lua 스크립트에서 기존 티켓 재확인, 순번 증가, 대기열·티켓·사용자 매핑 저장을 원자적으로 처리
        QueueTicket savedTicket = queueRedisStore.joinOrGet(
                candidate,
                expiration
        );

        return QueueJoinResponse.from(savedTicket);
    }


    // 사용자의 대기열 상태 조회
    public QueueStatusResult getQueueStatus(
            Long userId,
            Long eventId,
            Long sessionId
    ) {
        QueueTicketQueryResult queryResult =
                queueRedisStore
                        .findTicketStatus(
                                userId,
                                sessionId
                        )
                        .orElseThrow(
                                QueueNotFoundException::new
                        );

        QueueTicket ticket = queryResult.ticket();

        validateQueueTicketOwner(
                ticket,
                userId,
                eventId,
                sessionId
        );

        return createStatusResult(
                ticket,
                queryResult.aheadCount(),
                Instant.now()
        );
    }


    // 대기열 상태 조회 결과 -> 반환값 생성
    private QueueStatusResult createStatusResult(
            QueueTicket ticket,
            Long aheadCount,
            Instant now
    ) {
        return switch (ticket.status()) {
            case WAITING ->
                    handleWaiting(ticket, aheadCount);
            case SELECTING ->
                    handleSelecting(ticket, now);
            case CHECKOUT ->
                    QueueStatusResult.withoutToken(
                            QueueStatusResponse.checkout()
                    );
            case EXPIRED, CANCELLED ->
                    QueueStatusResult.withoutToken(
                            QueueStatusResponse.terminal(
                                    ticket.status()
                            )
                    );
        };
    }

    private QueueStatusResult handleWaiting(
            QueueTicket ticket,
            Long aheadCount
    ) {
        // Redis 데이터가 일관되지 않은 상태
        if (aheadCount == null) {
            throw new QueueUnavailableException(
                    new IllegalStateException(
                            "WAITING 티켓의 ZRANK가 없습니다."
                    )
            );
        }

        // 내부적으로 마지막 heartbeat 이후
        //HEARTBEAT_REFRESH_INTERVAL 이상 지났을 때만 갱신함
        queueHeartbeatRedisStore.touchIfNecessary(
                ticket.sessionId(),
                ticket.queueTicketId(),
                Instant.now()
        );

        long nextPollAfterMs = calculateNextPollAfterMs(aheadCount);

        return QueueStatusResult.withoutToken(
                QueueStatusResponse.waiting(
                        aheadCount,
                        nextPollAfterMs
                )
        );
    }

    private QueueStatusResult handleSelecting(
            QueueTicket ticket,
            Instant now
    ) {
        Instant selectingExpiresAt = ticket.selectingExpiresAt();

        if (selectingExpiresAt == null) {
            throw new QueueUnavailableException(
                    new IllegalStateException(
                            "SELECTING 티켓에 selectingExpiresAt이 없습니다."
                    )
            );
        }

        // 만료 시각과 동일하거나 이미 지났다면 EXPIRED 전환 시도
        if (!now.isBefore(selectingExpiresAt)) {
            queueRedisStore.expireSelectingIfCurrent(
                    ticket.userId(),
                    ticket.sessionId(),
                    ticket.queueTicketId(),
                    now
            );

            return QueueStatusResult.withoutToken(
                    QueueStatusResponse.terminal(
                            QueueStatus.EXPIRED
                    )
            );
        }

        // 예매 진행 페이지에 접근 가능한 토큰 발급
        Optional<AdmissionToken> admissionToken =
                admissionTokenService.issueIfAbsent(
                        ticket,
                        selectingExpiresAt
                );

        QueueStatusResponse response =
                QueueStatusResponse.selecting(
                        selectingExpiresAt
                );

        return admissionToken
                .map(token ->
                        QueueStatusResult.withToken( // 새 토큰이 생성됨
                                response,
                                token
                        )
                )
                .orElseGet(() ->
                        QueueStatusResult.withoutToken( // 이미 기존 토큰이 있음
                                response
                        )
                );
    }

    private void validateQueueTicketOwner(
            QueueTicket ticket,
            Long userId,
            Long eventId,
            Long sessionId
    ) {
        boolean sameUser = Objects.equals(
                ticket.userId(),
                userId
        );

        boolean sameEvent = Objects.equals(
                ticket.eventId(),
                eventId
        );

        boolean sameSession = Objects.equals(
                ticket.sessionId(),
                sessionId
        );

        if (!sameUser || !sameEvent || !sameSession) {
            // 티켓 존재 여부나 실제 소유자를 외부에 노출하지 않음
            throw new QueueNotFoundException();
        }
    }

    private long calculateNextPollAfterMs(
            long aheadCount
    ) {
        if (aheadCount >= 1_000L) {
            return 10_000L;
        }

        if (aheadCount >= 100L) {
            return 5_000L;
        }

        return 2_000L;
    }
}
