package com.pulse.PulsePass.dto.response;

import com.pulse.PulsePass.domain.enums.EventCategory;
import com.pulse.PulsePass.domain.enums.EventStatus;

import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long id,
        String eventCode,
        String name,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        String venueCode,
        String venueName) {}
