package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Artist;
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

/** FR-ART-001..004, FR-SRC-001, QT-005, QT-008, AC-003, AC-007. */
@PostgresDataJpaTest
class EventArtistIT {

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private Venue marina() {
        return venueRepository.saveAndFlush(TestData.marinaConventionCenter());
    }

    private Artist seeded(String stageName) {
        return artistRepository.findByStageName(stageName).orElseThrow();
    }

    @Test
    @DisplayName("FR-ART-001: un artista se persiste y se recupera")
    void persistsArtist() {
        artistRepository.saveAndFlush(new Artist("Test Artist", "Colombia", "Rock", true));
        entityManager.clear();

        Artist found = artistRepository.findByStageName("Test Artist").orElseThrow();

        assertThat(found.getCountry()).isEqualTo("Colombia");
        assertThat(found.getGenre()).isEqualTo("Rock");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    @DisplayName("FR-ART-002: PostgreSQL rechaza un stageName duplicado")
    void rejectsDuplicateStageName() {
        // "Solar Beat" ya fue insertado por la migracion V2
        assertThatThrownBy(() -> artistRepository.saveAndFlush(new Artist("Solar Beat", "Peru", "Pop", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("AC-003: un evento con tres artistas los conserva todos")
    void eventKeepsThreeArtists() {
        Event event = TestData.event("CMF-2026", marina(), EventStatus.PUBLISHED, TestData.FUTURE);
        event.addArtist(seeded("Solar Beat"));
        event.addArtist(seeded("Neon Waves"));
        event.addArtist(seeded("Caribbean Sound"));
        eventRepository.saveAndFlush(event);
        entityManager.clear();

        Event found = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThat(found.getArtists()).extracting(Artist::getStageName)
                .containsExactlyInAnyOrder("Solar Beat", "Neon Waves", "Caribbean Sound");
    }

    @Test
    @DisplayName("AC-003: agregar dos veces el mismo artista no duplica la asociacion")
    void sameArtistIsNotDuplicated() {
        Event event = TestData.event("EVT-ONCE", marina(), EventStatus.PUBLISHED, TestData.FUTURE);
        Artist solar = seeded("Solar Beat");
        event.addArtist(solar);
        event.addArtist(solar);
        eventRepository.saveAndFlush(event);

        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_artists WHERE event_id = ?", Integer.class, event.getId());

        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-ART-003: la PK compuesta rechaza repetir el par evento-artista")
    void compositeKeyRejectsDuplicatePair() {
        Event event = TestData.event("EVT-PAIR", marina(), EventStatus.PUBLISHED, TestData.FUTURE);
        Artist solar = seeded("Solar Beat");
        event.addArtist(solar);
        eventRepository.saveAndFlush(event);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO event_artists (event_id, artist_id) VALUES (?, ?)", event.getId(), solar.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("AC-007 / FR-SRC-001: cada evento de Solar Beat aparece una sola vez y ordenado")
    void findsEventsByArtistWithoutDuplicates() {
        Venue venue = marina();
        Artist solar = seeded("Solar Beat");
        Artist neon = seeded("Neon Waves");

        Event first = TestData.event("EVT-A1", venue, EventStatus.PUBLISHED, LocalDateTime.of(2030, 8, 1, 20, 0));
        first.addArtist(solar);
        first.addArtist(neon);
        Event second = TestData.event("EVT-A2", venue, EventStatus.PUBLISHED, LocalDateTime.of(2030, 2, 1, 20, 0));
        second.addArtist(solar);
        Event third = TestData.event("EVT-A3", venue, EventStatus.PUBLISHED, LocalDateTime.of(2030, 5, 1, 20, 0));
        third.addArtist(neon);
        eventRepository.save(first);
        eventRepository.save(second);
        eventRepository.save(third);
        eventRepository.flush();

        List<Event> events = eventRepository.findByArtistStageName("Solar Beat");

        assertThat(events).extracting(Event::getEventCode).containsExactly("EVT-A2", "EVT-A1");
    }

    @Test
    @DisplayName("FR-ART-004: un artista sin eventos retorna lista vacia")
    void artistWithoutEventsReturnsEmpty() {
        assertThat(eventRepository.findByArtistStageName("Digital Pulse")).isEmpty();
    }
}
