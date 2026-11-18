package com.example.ticketing.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal x1;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal y1;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal x2;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal y2;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal x3;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal y3;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal x4;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal y4;

    @Column(length = 20)
    private String color;
}
