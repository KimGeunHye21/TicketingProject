package com.example.ticketing.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "place_name", nullable = false, length = 255)
    private String placeName;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "running_time", nullable = false)
    private Integer runningTime;

    @Column(length = 1000)
    private String cast;

    @Column(name = "max_ticket_per_user", nullable = false)
    private Integer maxTicketPerUser;

    @Column(name = "booking_open_at", nullable = false)
    private LocalDateTime bookingOpenAt;

    @Column(name = "seat_map_view_box", length = 100)
    private String seatMapViewBox;
}