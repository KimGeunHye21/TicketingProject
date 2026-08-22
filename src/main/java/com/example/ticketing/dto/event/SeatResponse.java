package com.example.ticketing.dto.event;

import com.example.ticketing.domain.Seat;
import com.example.ticketing.domain.SeatInstance;
import com.example.ticketing.domain.SeatStatus;

public record SeatResponse(
        Long seatInstanceId,
        Long seatId,
        Long sectionId,
        String seatRow,
        Integer seatNo,
        Integer x,
        Integer y,
        boolean reservable
) {

    public static SeatResponse from(SeatInstance seatInstance) {
        Seat seat = seatInstance.getSeat();

        boolean reservable =
                seatInstance.getStatus() == SeatStatus.AVAILABLE;

        return new SeatResponse(
                seatInstance.getId(),
                seat.getId(),
                seat.getSection().getId(),
                seat.getSeatRow(),
                seat.getSeatNo(),
                seat.getX(),
                seat.getY(),
                reservable
        );
    }
}
