package com.example.ticketing.dto.event;

import com.example.ticketing.domain.Section;
import java.util.List;

public record SectionResponse(
        Long sectionId,
        String name,
        int price,
        long availableSeatCount,
        List<SectionPointResponse> points,
        String color
) {

    public static SectionResponse from(
            Section section,
            long availableSeatCount
    ) {
        return new SectionResponse(
                section.getId(),
                section.getName(),
                section.getPrice(),
                availableSeatCount,
                List.of(
                        new SectionPointResponse(section.getX1(), section.getY1()),
                        new SectionPointResponse(section.getX2(), section.getY2()),
                        new SectionPointResponse(section.getX3(), section.getY3()),
                        new SectionPointResponse(section.getX4(), section.getY4())
                ),
                section.getColor()
        );
    }
}