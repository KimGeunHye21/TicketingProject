package com.example.ticketing.dto.event;

import java.util.List;

public record SectionMapResponse(
        Long eventId,
        String viewBox,
        List<SectionResponse> sections
) {
}
