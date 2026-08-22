package com.example.ticketing.repository;

import com.example.ticketing.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllByEvent_IdOrderByIdAsc(Long eventId);
}
