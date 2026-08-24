package com.example.ticketing.dto.event;

import com.example.ticketing.domain.Section;

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

    public static SectionResponse from(
            Section section,
            long availableSeatCount
    ) {
        return new SectionResponse(
                section.getId(),
                section.getName(),
                section.getPrice(),
                availableSeatCount,
                section.getSvgPath(),
                section.getLabelX(),
                section.getLabelY(),
                section.getColor()
        );
    }
}
