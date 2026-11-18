package com.pulse.PulsePass.dto.request;

import com.pulse.PulsePass.domain.enums.EventCategory;

import java.time.LocalDateTime;

public record CreateEventRequest(
        String eventCode,
        String name,
        String description,
        EventCategory category,
        LocalDateTime eventDate,
        Integer minimumAge,
        String venueCode
) {}
