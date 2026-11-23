package com.pulse.PulsePass.service;

import com.pulse.PulsePass.domain.Artist;
import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.domain.enums.EventCategory;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.dto.request.CreateEventRequest;
import com.pulse.PulsePass.dto.response.EventResponse;
import com.pulse.PulsePass.exception.BusinessRuleException;
import com.pulse.PulsePass.exception.DuplicateResourceException;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.mapper.EventMapper;
import com.pulse.PulsePass.repository.ArtistRepository;
import com.pulse.PulsePass.repository.EventRepository;
import com.pulse.PulsePass.repository.VenueRepository;
import com.pulse.PulsePass.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private EventMapper eventMapper;

    private EventServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new EventServiceImpl(
                        eventRepository,
                        venueRepository,
                        artistRepository,
                        eventMapper,
                        ServiceTestData.CLOCK);
    }

    private EventResponse response(EventStatus status) {
        return new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival",
                EventCategory.MUSIC,
                status,
                ServiceTestData.EVENT_DATE,
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of());
    }

    private CreateEventRequest request(LocalDateTime date) {
        return new CreateEventRequest(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival",
                EventCategory.MUSIC,
                date,
                18,
                "VEN-SMR-01");
    }

    // TEST-EVENT-001
    @Test
    void findByCode_existingEvent_returnsDto() {
        Event event = ServiceTestData.event(EventStatus.DRAFT, ServiceTestData.venue(true));
        EventResponse expected = response(EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expected);

        EventResponse result = service.findByCode("CMF-2026");

        assertThat(result).isEqualTo(expected);
    }

    // TEST-EVENT-002
    @Test
    void findByCode_missingEvent_throwsResourceNotFound() {
        when(eventRepository.findByEventCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: NOPE");
    }

    // TEST-EVENT-003 / BR-EVENT-005
    @Test
    void create_validRequest_savesEventInDraft() {
        Venue venue = ServiceTestData.venue(true);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response(EventStatus.DRAFT));

        EventResponse result = service.create(request(ServiceTestData.EVENT_DATE));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(captor.getValue().getVenue()).isSameAs(venue);
        assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void create_duplicatedCode_throwsDuplicateAndDoesNotSave() {
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(ServiceTestData.EVENT_DATE)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-004
    @Test
    void create_missingVenue_throwsResourceNotFoundAndDoesNotSave() {
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(ServiceTestData.EVENT_DATE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-005
    @Test
    void create_inactiveVenue_throwsBusinessRule() {
        when(venueRepository.findByCode("VEN-SMR-01"))
                .thenReturn(Optional.of(ServiceTestData.venue(false)));

        assertThatThrownBy(() -> service.create(request(ServiceTestData.EVENT_DATE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-006
    @Test
    void create_pastDate_throwsBusinessRule() {
        when(venueRepository.findByCode("VEN-SMR-01"))
                .thenReturn(Optional.of(ServiceTestData.venue(true)));

        assertThatThrownBy(() -> service.create(request(LocalDateTime.of(2026, 1, 1, 20, 0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        verify(eventRepository, never()).save(any());
    }

    // BR-EVENT-006
    @Test
    void create_negativeMinimumAge_throwsBusinessRule() {
        when(venueRepository.findByCode("VEN-SMR-01"))
                .thenReturn(Optional.of(ServiceTestData.venue(true)));
        CreateEventRequest negativeAge =
                new CreateEventRequest(
                        "CMF-2026",
                        "Fest",
                        "Festival",
                        EventCategory.MUSIC,
                        ServiceTestData.EVENT_DATE,
                        -1,
                        "VEN-SMR-01");

        assertThatThrownBy(() -> service.create(negativeAge))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-007
    @Test
    void publish_validDraft_changesStatusToPublished() {
        Event event = ServiceTestData.event(EventStatus.DRAFT, ServiceTestData.venue(true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(response(EventStatus.PUBLISHED));

        EventResponse result = service.publish("CMF-2026");

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
    }

    // TEST-EVENT-008
    @Test
    void publish_cancelledEvent_throwsBusinessRuleAndDoesNotPersist() {
        Event event = ServiceTestData.event(EventStatus.CANCELLED, ServiceTestData.venue(true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository, never()).save(any());
    }

    // BR-EVENT-008
    @Test
    void publish_pastDate_throwsBusinessRule() {
        Event event = ServiceTestData.event(EventStatus.DRAFT, ServiceTestData.venue(true));
        event.setEventDate(LocalDateTime.of(2026, 1, 1, 20, 0));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // BR-EVENT-009
    @Test
    void publish_inactiveVenue_throwsBusinessRule() {
        Event event = ServiceTestData.event(EventStatus.DRAFT, ServiceTestData.venue(false));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // FR-SVC-007 / BR-EVENT-010..011
    @Test
    void addArtist_validArtist_associatesAndSaves() {
        Event event = ServiceTestData.event(EventStatus.DRAFT, ServiceTestData.venue(true));
        Artist artist = ServiceTestData.artist("Solar Beat");
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(response(EventStatus.DRAFT));

        service.addArtist("CMF-2026", 1L);

        assertThat(event.getArtists()).containsExactly(artist);
        verify(eventRepository).save(event);
    }

    @Test
    void addArtist_missingArtist_throwsResourceNotFound() {
        Event event = ServiceTestData.event(EventStatus.DRAFT, ServiceTestData.venue(true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addArtist("CMF-2026", 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_alreadyAssociated_throwsDuplicate() {
        Event event = ServiceTestData.event(EventStatus.DRAFT, ServiceTestData.venue(true));
        Artist artist = ServiceTestData.artist("Solar Beat");
        event.addArtist(artist);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_cancelledEvent_throwsBusinessRule() {
        Event event = ServiceTestData.event(EventStatus.CANCELLED, ServiceTestData.venue(true));
        Artist artist = ServiceTestData.artist("Solar Beat");
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void findPublishedEvents_queriesPublishedStatus() {
        when(eventRepository.findByStatusOrderByEventDateAsc(eq(EventStatus.PUBLISHED)))
                .thenReturn(List.of());

        assertThat(service.findPublishedEvents()).isEmpty();
    }
}
