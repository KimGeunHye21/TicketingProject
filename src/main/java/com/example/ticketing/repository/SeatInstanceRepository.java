package com.example.ticketing.repository;

import com.example.ticketing.domain.SeatInstance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatInstanceRepository extends JpaRepository<SeatInstance, Long> {

    long countBySession_Id(Long sessionId);

    @EntityGraph(attributePaths = {"session", "session.event"})
    Optional<SeatInstance> findWithSessionById(Long id);

    @Query("""
            select si.id
            from SeatInstance si
            where si.session.id = :sessionId
              and (:sectionId is null or si.seat.section.id = :sectionId)
            order by si.id
            """)
    List<Long> findIdsBySessionAndSection(
            @Param("sessionId") Long sessionId,
            @Param("sectionId") Long sectionId
    );
}
