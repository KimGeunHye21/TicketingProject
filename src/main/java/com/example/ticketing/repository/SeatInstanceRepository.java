package com.example.ticketing.repository;

import com.example.ticketing.domain.SeatInstance;
import com.example.ticketing.domain.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatInstanceRepository
        extends JpaRepository<SeatInstance, Long> {

    boolean existsBySession_IdAndStatus(
            Long sessionId,
            SeatStatus status
    );
}
