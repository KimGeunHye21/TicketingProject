package com.example.ticketing.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "seat",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_seat_position",
                        columnNames = {
                                "section_id",
                                "seat_row",
                                "seat_no"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "seat_row", nullable = false, length = 20)
    private String seatRow;

    @Column(name = "seat_no", nullable = false)
    private Integer seatNo;

    @Column(nullable = false)
    private Integer x;

    @Column(nullable = false)
    private Integer y;
}
