package com.pulse.PulsePass.service.impl;

import com.pulse.PulsePass.domain.Artist;
import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.dto.request.CreateEventRequest;
import com.pulse.PulsePass.dto.response.EventResponse;
import com.pulse.PulsePass.dto.response.EventSummaryResponse;
import com.pulse.PulsePass.exception.BusinessRuleException;
import com.pulse.PulsePass.exception.DuplicateResourceException;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.mapper.EventMapper;
import com.pulse.PulsePass.repository.ArtistRepository;
import com.pulse.PulsePass.repository.EventRepository;
import com.pulse.PulsePass.repository.VenueRepository;
import com.pulse.PulsePass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;
    private final Clock clock;

    public EventServiceImpl(EventRepository eventRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper eventMapper,
                            Clock clock) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        validateRequiredFields(request);

        // BR-EVENT-001
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event already exists: " + request.eventCode());
        }
        // BR-EVENT-002
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));
        // BR-EVENT-003
        if (!venue.isActive()) {
            throw new BusinessRuleException("Venue is not active: " + venue.getCode());
        }
        // BR-EVENT-004
        if (!request.eventDate().isAfter(now())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }
        // BR-EVENT-006
        if (request.minimumAge() < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative.");
        }

        // BR-EVENT-005: el estado inicial no lo controla el request
        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.category(),
                EventStatus.DRAFT,
                request.eventDate(),
                request.minimumAge(),
                venue);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(String eventCode) {
        return eventMapper.toResponse(getEvent(eventCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = getEvent(eventCode);

        // BR-EVENT-007
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Current status: " + event.getStatus());
        }
        // BR-EVENT-008
        if (!event.getEventDate().isAfter(now())) {
            throw new BusinessRuleException("Event date must be in the future to publish: " + eventCode);
        }
        // BR-EVENT-009
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException("Venue is not active: " + event.getVenue().getCode());
        }

        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = getEvent(eventCode);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));

        // BR-EVENT-011
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Cannot add artists to a " + event.getStatus() + " event: " + eventCode);
        }
        // BR-EVENT-010
        if (event.getArtists().contains(artist)) {
            throw new DuplicateResourceException(
                    "Artist " + artist.getStageName() + " is already associated to event " + eventCode);
        }

        event.addArtist(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findByArtistStageName(stageName)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    private Event getEvent(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private void validateRequiredFields(CreateEventRequest request) {
        if (request == null
                || isBlank(request.eventCode())
                || isBlank(request.name())
                || isBlank(request.venueCode())
                || request.category() == null
                || request.eventDate() == null
                || request.minimumAge() == null) {
            throw new BusinessRuleException(
                    "eventCode, name, category, eventDate, minimumAge and venueCode are required.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
