package com.example.ticketing.service;

import com.example.ticketing.domain.Event;
import com.example.ticketing.domain.EventSession;
import com.example.ticketing.dto.queue.QueueJoinResponse;
import com.example.ticketing.exception.BookingNotOpenException;
import com.example.ticketing.exception.EventNotFoundException;
import com.example.ticketing.exception.EventSessionNotFoundException;
import com.example.ticketing.queue.QueueRedisStore;
import com.example.ticketing.queue.domain.QueueTicket;
import com.example.ticketing.repository.EventRepository;
import com.example.ticketing.repository.EventSessionRepository;
import com.example.ticketing.repository.SeatInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueService {

    private static final Duration QUEUE_GRACE_PERIOD = Duration.ofHours(1);

    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;
    private final SeatInstanceRepository seatInstanceRepository;
    private final QueueRedisStore queueRedisStore;

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
        LocalDateTime now = LocalDateTime.now();
        validateBookingTime(event, session, now);


        // 동일 사용자·동일 회차의 활성 대기열 티켓이 있으면 그대로 반환
        // 없으면 새로운 대기열 티켓 생성
        return queueRedisStore.findActiveTicket(userId, sessionId)
                .map(QueueJoinResponse::from)
                .orElseGet(() -> registerQueueTicket(
                        userId,
                        eventId,
                        session,
                        now
                ));
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
            LocalDateTime now
    ) {
        QueueTicket candidate = QueueTicket.waiting(
                UUID.randomUUID().toString(), // 예측 불가능한 queueTicketId 발급
                userId,
                eventId,
                session.getId(),
                now
        );

        Duration expiration = Duration.between(
                now,
                session.getStartAt()
        ).plus(QUEUE_GRACE_PERIOD);

        // Lua 스크립트에서 기존 티켓 재확인, 순번 증가, 대기열·티켓·사용자 매핑 저장을 원자적으로 처리
        QueueTicket savedTicket = queueRedisStore.joinOrGet(
                candidate,
                expiration
        );

        return QueueJoinResponse.from(savedTicket);
    }
}
