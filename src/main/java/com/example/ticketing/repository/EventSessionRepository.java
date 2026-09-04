package com.example.ticketing.repository;

import com.example.ticketing.domain.EventSession;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
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


    // 대기열 입장 처리가 필요한 활성 회차 ID 조회
    @Query("""
            select session.id
            from EventSession session
            join session.event event
            where event.bookingOpenAt <= :now
              and session.startAt > :now
            order by session.id
            """)
    List<Long> findActiveQueueSessionIds(
            @Param("now")
            LocalDateTime now
    );
}
