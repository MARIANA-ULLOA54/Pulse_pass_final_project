package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Artist;
import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.Ticket;
import com.pulse.PulsePass.domain.User;
import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.domain.enums.TicketStatus;
import com.pulse.PulsePass.domain.enums.TicketType;
import com.pulse.PulsePass.support.PostgresDataJpaTest;
import com.pulse.PulsePass.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-SRC-001..004, UC-07, UC-09, QT-008. */
@PostgresDataJpaTest
class EventSearchIT {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 17, 0, 0);

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        Venue santaMarta = venueRepository.save(TestData.marinaConventionCenter());
        Venue bogota = venueRepository.save(TestData.venue("VEN-BOG-01", "Bogota", 3000));

        Artist solar = artistRepository.findByStageName("Solar Beat").orElseThrow();
        Artist neon = artistRepository.findByStageName("Neon Waves").orElseThrow();

        // Dos artistas cuyo nombre contiene "o": sin DISTINCT este evento saldria duplicado
        Event cmf = TestData.event("CMF-2026", santaMarta, EventStatus.PUBLISHED, LocalDateTime.of(2030, 8, 1, 20, 0));
        cmf.addArtist(solar);
        cmf.addArtist(neon);

        Event soon = TestData.event("EVT-SOON", santaMarta, EventStatus.PUBLISHED, LocalDateTime.of(2030, 2, 1, 20, 0));
        soon.addArtist(solar);

        Event draft = TestData.event("EVT-DRAFT", santaMarta, EventStatus.DRAFT, LocalDateTime.of(2030, 4, 1, 20, 0));
        draft.addArtist(solar);

        Event past = TestData.event("EVT-PAST", santaMarta, EventStatus.PUBLISHED, LocalDateTime.of(2020, 4, 1, 20, 0));
        past.addArtist(solar);

        Event inBogota = TestData.event("EVT-BOG", bogota, EventStatus.PUBLISHED, LocalDateTime.of(2030, 3, 1, 20, 0));
        inBogota.addArtist(solar);

        eventRepository.save(cmf);
        eventRepository.save(soon);
        eventRepository.save(draft);
        eventRepository.save(past);
        eventRepository.save(inBogota);

        User andrea = userRepository.save(TestData.user("andrea", "andrea@pulse.com"));
        ticketRepository.save(TestData.ticket("TCK-CMF", andrea, cmf, TicketType.VIP, TicketStatus.PAID, "250000"));
        ticketRepository.save(TestData.ticket("TCK-SOON", andrea, soon, TicketType.GENERAL, TicketStatus.PAID, "120000"));
        ticketRepository.save(TestData.ticket("TCK-PAST", andrea, past, TicketType.GENERAL, TicketStatus.USED, "100000"));
        ticketRepository.flush();
    }

    @Test
    @DisplayName("FR-SRC-002: eventos de una ciudad en los que participa el artista")
    void findsEventsByCityAndArtist() {
        List<Event> events = eventRepository.findByCityAndArtistStageName("Bogota", "Solar Beat");

        assertThat(events).extracting(Event::getEventCode).containsExactly("EVT-BOG");
    }

    @Test
    @DisplayName("FR-SRC-002: filtra por venue.city y por artist.stageName a la vez")
    void cityAndArtistBothMustMatch() {
        assertThat(eventRepository.findByCityAndArtistStageName("Bogota", "Neon Waves")).isEmpty();
        assertThat(eventRepository.findByCityAndArtistStageName("Santa Marta", "Neon Waves"))
                .extracting(Event::getEventCode).containsExactly("CMF-2026");
    }

    @Test
    @DisplayName("FR-SRC-003: recomendados = publicados, posteriores a la fecha, de la ciudad, sin duplicados y ordenados")
    void findsRecommendedEvents() {
        List<Event> events = eventRepository.findRecommendedEvents(
                EventStatus.PUBLISHED, NOW, "Santa Marta", "o");

        // CMF-2026 tiene dos artistas que contienen "o" y aun asi aparece una sola vez
        assertThat(events).extracting(Event::getEventCode).containsExactly("EVT-SOON", "CMF-2026");
    }

    @Test
    @DisplayName("FR-SRC-003: el texto del artista se compara sin distinguir mayusculas")
    void recommendedIsCaseInsensitiveOnArtist() {
        List<Event> events = eventRepository.findRecommendedEvents(
                EventStatus.PUBLISHED, NOW, "Santa Marta", "SOLAR");

        assertThat(events).extracting(Event::getEventCode).containsExactly("EVT-SOON", "CMF-2026");
    }

    @Test
    @DisplayName("FR-SRC-003: excluye borradores, eventos pasados y otras ciudades")
    void recommendedExcludesDraftPastAndOtherCities() {
        List<Event> events = eventRepository.findRecommendedEvents(
                EventStatus.PUBLISHED, NOW, "Santa Marta", "solar");

        assertThat(events).extracting(Event::getEventCode)
                .doesNotContain("EVT-DRAFT", "EVT-PAST", "EVT-BOG");
    }

    @Test
    @DisplayName("FR-SRC-004: tickets de eventos futuros en orden cronologico")
    void findsTicketsOfFutureEventsInChronologicalOrder() {
        List<Ticket> tickets = ticketRepository.findTicketsOfEventsAfter(NOW);

        assertThat(tickets).extracting(Ticket::getTicketCode).containsExactly("TCK-SOON", "TCK-CMF");
    }
}
