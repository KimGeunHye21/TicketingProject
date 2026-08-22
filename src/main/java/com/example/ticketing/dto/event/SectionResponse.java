package com.example.ticketing.dto.event;

public record SectionResponse(
        Long sectionId,
        String name,
        int price,
        long availableSeatCount,
        String svgPath,
        Integer labelX,
        Integer labelY,
        String color
) {
}
