package com.pulse.PulsePass.dto.response;

import com.pulse.PulsePass.domain.enums.EventCategory;
import com.pulse.PulsePass.domain.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
        Long id,
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        int minimumAge,
        String venueCode,
        String venueName,
        List<ArtistResponse> artists) {}
