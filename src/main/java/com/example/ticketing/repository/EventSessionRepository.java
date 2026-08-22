package com.example.ticketing.repository;

import com.example.ticketing.domain.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSessionRepository
        extends JpaRepository<EventSession, Long> {

    List<EventSession> findAllByEvent_IdOrderByStartAtAsc(Long eventId);
}
