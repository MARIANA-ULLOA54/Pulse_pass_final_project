package com.pulse.PulsePass.service;

import com.pulse.PulsePass.dto.request.CreateEventRequest;
import com.pulse.PulsePass.dto.response.EventResponse;
import com.pulse.PulsePass.dto.response.EventSummaryResponse;

import java.util.List;

public interface EventService {

    EventResponse create(CreateEventRequest request);

    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    EventResponse publish(String eventCode);

    EventResponse addArtist(String eventCode, Long artistId);

    List<EventSummaryResponse> findByArtist(String stageName);
}
