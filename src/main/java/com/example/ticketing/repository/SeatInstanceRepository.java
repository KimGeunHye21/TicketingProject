package com.example.ticketing.repository;

import com.example.ticketing.domain.SeatInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatInstanceRepository
        extends JpaRepository<SeatInstance, Long> {

    long countBySession_Id(Long sessionId);
}
