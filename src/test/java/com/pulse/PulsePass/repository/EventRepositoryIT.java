package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.support.PostgresDataJpaTest;
import com.pulse.PulsePass.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-EVT-001..006, FR-VEN-004, QT-003, QT-007, AC-002, AC-006. */
@PostgresDataJpaTest
class EventRepositoryIT {

    @Autowired private EventRepository eventRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private Venue marina() {
        return venueRepository.saveAndFlush(TestData.marinaConventionCenter());
    }

    @Test
    @DisplayName("AC-002 / QT-003: el evento se recupera por eventCode junto con su venue")
    void findsEventByCodeWithVenue() {
        Venue venue = marina();
        eventRepository.saveAndFlush(
                TestData.event("CMF-2026", venue, EventStatus.PUBLISHED, TestData.FUTURE));
        entityManager.clear();

        Event found = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThat(found.getVenue().getCode()).isEqualTo("VEN-SMR-01");
        assertThat(found.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("FR-EVT-002: PostgreSQL rechaza un eventCode duplicado")
    void rejectsDuplicateEventCode() {
        Venue venue = marina();
        eventRepository.saveAndFlush(
                TestData.event("EVT-DUP", venue, EventStatus.DRAFT, TestData.FUTURE));

        assertThatThrownBy(
                        () ->
                                eventRepository.saveAndFlush(
                                        TestData.event(
                                                "EVT-DUP",
                                                venue,
                                                EventStatus.DRAFT,
                                                TestData.FUTURE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("AC-006 / FR-EVT-005: solo PUBLISHED y en orden de fecha ascendente")
    void findsOnlyPublishedOrderedByDate() {
        Venue venue = marina();
        eventRepository.save(
                TestData.event(
                        "EVT-LATE",
                        venue,
                        EventStatus.PUBLISHED,
                        LocalDateTime.of(2030, 9, 1, 20, 0)));
        eventRepository.save(
                TestData.event(
                        "EVT-EARLY",
                        venue,
                        EventStatus.PUBLISHED,
                        LocalDateTime.of(2030, 3, 1, 20, 0)));
        eventRepository.save(
                TestData.event(
                        "EVT-DRAFT",
                        venue,
                        EventStatus.DRAFT,
                        LocalDateTime.of(2030, 4, 1, 20, 0)));
        eventRepository.save(
                TestData.event(
                        "EVT-CANCELLED",
                        venue,
                        EventStatus.CANCELLED,
                        LocalDateTime.of(2030, 5, 1, 20, 0)));
        eventRepository.flush();

        List<Event> published =
                eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(published)
                .extracting(Event::getEventCode)
                .containsExactly("EVT-EARLY", "EVT-LATE");
    }

    @Test
    @DisplayName("FR-VEN-004: los eventos por codigo de venue incluyen solo los de ese venue")
    void findsEventsByVenueCode() {
        Venue marina = marina();
        Venue other = venueRepository.saveAndFlush(TestData.venue("VEN-BOG-01", "Bogota", 800));
        eventRepository.save(
                TestData.event("EVT-SMR-1", marina, EventStatus.PUBLISHED, TestData.FUTURE));
        eventRepository.save(
                TestData.event("EVT-SMR-2", marina, EventStatus.DRAFT, TestData.FUTURE));
        eventRepository.save(
                TestData.event("EVT-BOG-1", other, EventStatus.PUBLISHED, TestData.FUTURE));
        eventRepository.flush();

        List<Event> events = eventRepository.findByVenue_Code("VEN-SMR-01");

        assertThat(events)
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("EVT-SMR-1", "EVT-SMR-2");
    }

    @Test
    @DisplayName("FR-EVT-006: streamingUrl es opcional y se persiste cuando existe")
    void streamingUrlIsOptional() {
        Venue venue = marina();
        Event onsite = TestData.event("EVT-ONSITE", venue, EventStatus.PUBLISHED, TestData.FUTURE);
        Event hybrid = TestData.event("EVT-HYBRID", venue, EventStatus.PUBLISHED, TestData.FUTURE);
        hybrid.setStreamingUrl("https://stream.pulsepass.test/hybrid");
        eventRepository.save(onsite);
        eventRepository.save(hybrid);
        eventRepository.flush();
        entityManager.clear();

        assertThat(eventRepository.findByEventCode("EVT-ONSITE").orElseThrow().getStreamingUrl())
                .isNull();
        assertThat(eventRepository.findByEventCode("EVT-HYBRID").orElseThrow().getStreamingUrl())
                .isEqualTo("https://stream.pulsepass.test/hybrid");
    }

    @Test
    @DisplayName("FR-EVT-004 / BR-008: categoria y estado se almacenan por nombre, no por ordinal")
    void enumsAreStoredByName() {
        Venue venue = marina();
        eventRepository.saveAndFlush(
                TestData.event("EVT-ENUM", venue, EventStatus.SOLD_OUT, TestData.FUTURE));

        String category =
                jdbc.queryForObject(
                        "SELECT category FROM events WHERE event_code = 'EVT-ENUM'", String.class);
        String status =
                jdbc.queryForObject(
                        "SELECT status FROM events WHERE event_code = 'EVT-ENUM'", String.class);

        assertThat(category).isEqualTo("MUSIC");
        assertThat(status).isEqualTo("SOLD_OUT");
    }

    @Test
    @DisplayName("FR-EVT-003: PostgreSQL rechaza un estado fuera del catalogo")
    void rejectsInvalidStatus() {
        Venue venue = marina();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO events (event_code, name, category, status, event_date, minimum_age, venue_id) "
                                                + "VALUES ('EVT-BAD', 'Malo', 'MUSIC', 'ARCHIVED', now(), 0, ?)",
                                        venue.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("BR-001: un evento sin venue valido es rechazado por la FK")
    void rejectsEventWithUnknownVenue() {
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO events (event_code, name, category, status, event_date, minimum_age, venue_id) "
                                                + "VALUES ('EVT-ORPHAN', 'Huerfano', 'MUSIC', 'DRAFT', now(), 0, -1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
