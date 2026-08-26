package com.example.ticketing.repository;

import com.example.ticketing.domain.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventSessionRepository
        extends JpaRepository<EventSession, Long> {

    List<EventSession> findAllByEvent_IdOrderByStartAtAsc(Long eventId);

    Optional<EventSession> findByIdAndEvent_Id(
            Long sessionId,
            Long eventId
    );

    // 이벤트 시작/종료 날자를 가져옴
    Optional<EventSession>
    findFirstByEvent_IdOrderByStartAtAsc(Long eventId);
    Optional<EventSession>
    findFirstByEvent_IdOrderByStartAtDesc(Long eventId);
}
