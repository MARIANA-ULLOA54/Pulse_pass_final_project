package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.Ticket;
import com.pulse.PulsePass.domain.User;
import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.domain.enums.TicketStatus;
import com.pulse.PulsePass.domain.enums.TicketType;
import com.pulse.PulsePass.support.PostgresDataJpaTest;
import com.pulse.PulsePass.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-TKT-001..008, UC-05, UC-08, QT-006, QT-007, QT-008, AC-005, AC-008. */
@PostgresDataJpaTest
class TicketRepositoryIT {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private Event event;
    private User andrea;
    private User carlos;
    private User laura;
    private User miguel;

    /** Escenario de la seccion 16 del PRD: evento CMF-2026 con cuatro tickets. */
    @BeforeEach
    void setUp() {
        Venue venue = venueRepository.save(TestData.marinaConventionCenter());
        event = eventRepository.save(TestData.event("CMF-2026", venue, EventStatus.PUBLISHED, TestData.FUTURE));
        andrea = userRepository.save(TestData.user("andrea", "andrea@pulse.com"));
        carlos = userRepository.save(TestData.user("carlos", "carlos@pulse.com"));
        laura = userRepository.save(TestData.user("laura", "laura@pulse.com"));
        miguel = userRepository.save(TestData.user("miguel", "miguel@pulse.com"));

        ticketRepository.save(TestData.ticket("TCK-0001", andrea, event, TicketType.VIP, TicketStatus.PAID, "250000"));
        ticketRepository.save(TestData.ticket("TCK-0002", carlos, event, TicketType.GENERAL, TicketStatus.PAID, "120000"));
        ticketRepository.save(TestData.ticket("TCK-0003", laura, event, TicketType.GENERAL, TicketStatus.RESERVED, "120000"));
        ticketRepository.save(TestData.ticket("TCK-0004", miguel, event, TicketType.VIP, TicketStatus.CANCELLED, "250000"));
        ticketRepository.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("QT-006: el ticket navega hacia su usuario y su evento")
    void ticketNavigatesToUserAndEvent() {
        Ticket ticket = ticketRepository.findByUser_Email("andrea@pulse.com").get(0);

        assertThat(ticket.getUser().getUsername()).isEqualTo("andrea");
        assertThat(ticket.getEvent().getEventCode()).isEqualTo("CMF-2026");
        assertThat(ticket.getType()).isEqualTo(TicketType.VIP);
        assertThat(ticket.getPrice()).isEqualByComparingTo("250000");
        assertThat(ticket.getPurchaseDate()).isNotNull();
    }

    @Test
    @DisplayName("AC-005: PostgreSQL rechaza un segundo ticket con el mismo ticketCode")
    void rejectsDuplicateTicketCode() {
        User user = userRepository.findByUsername("laura").orElseThrow();
        Event ev = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(
                TestData.ticket("TCK-0001", user, ev, TicketType.STUDENT, TicketStatus.RESERVED, "50000")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-003: el CHECK impide precios negativos y permite precio cero")
    void priceMustNotBeNegative() {
        User user = userRepository.findByUsername("laura").orElseThrow();
        Event ev = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        Ticket free = ticketRepository.saveAndFlush(
                TestData.ticket("TCK-FREE", user, ev, TicketType.STUDENT, TicketStatus.PAID, "0"));
        assertThat(free.getId()).isNotNull();

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(
                TestData.ticket("TCK-NEG", user, ev, TicketType.GENERAL, TicketStatus.PAID, "-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-001: la BD rechaza un ticket cuyo usuario o evento no existe")
    void rejectsTicketWithoutValidForeignKeys() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO tickets (ticket_code, type, price, status, purchase_date, user_id, event_id) "
                        + "VALUES ('TCK-FK', 'GENERAL', 10, 'PAID', now(), -1, -1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-004: la BD rechaza un tipo de ticket fuera del catalogo")
    void rejectsInvalidType() {
        Long userId = andrea.getId();
        Long eventId = event.getId();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO tickets (ticket_code, type, price, status, purchase_date, user_id, event_id) "
                        + "VALUES ('TCK-T', 'PLATINUM', 10, 'PAID', now(), ?, ?)", userId, eventId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-005: la BD rechaza un estado de ticket fuera del catalogo")
    void rejectsInvalidStatus() {
        Long userId = andrea.getId();
        Long eventId = event.getId();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO tickets (ticket_code, type, price, status, purchase_date, user_id, event_id) "
                        + "VALUES ('TCK-S', 'GENERAL', 10, 'REFUNDED', now(), ?, ?)", userId, eventId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("BR-007: el precio se almacena como NUMERIC con dos decimales")
    void priceKeepsDecimalPrecision() {
        User user = userRepository.findByUsername("laura").orElseThrow();
        Event ev = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        ticketRepository.saveAndFlush(
                TestData.ticket("TCK-DEC", user, ev, TicketType.GENERAL, TicketStatus.PAID, "99999.99"));
        entityManager.clear();

        BigDecimal stored = jdbc.queryForObject(
                "SELECT price FROM tickets WHERE ticket_code = 'TCK-DEC'", BigDecimal.class);

        assertThat(stored).isEqualByComparingTo("99999.99");
    }

    @Test
    @DisplayName("FR-TKT-006: tickets de un usuario por email")
    void findsTicketsByUserEmail() {
        List<Ticket> tickets = ticketRepository.findByUser_Email("carlos@pulse.com");

        assertThat(tickets).extracting(Ticket::getTicketCode).containsExactly("TCK-0002");
    }

    @Test
    @DisplayName("FR-TKT-006: tickets de un usuario por email y estado")
    void findsTicketsByUserEmailAndStatus() {
        assertThat(ticketRepository.findByUser_EmailAndStatus("laura@pulse.com", TicketStatus.RESERVED))
                .extracting(Ticket::getTicketCode).containsExactly("TCK-0003");
        assertThat(ticketRepository.findByUser_EmailAndStatus("laura@pulse.com", TicketStatus.PAID)).isEmpty();
    }

    @Test
    @DisplayName("FR-TKT-007: solo se retornan los tickets PAID del evento solicitado")
    void findsPaidTicketsByEventCode() {
        List<Ticket> paid = ticketRepository.findByEvent_EventCodeAndStatus("CMF-2026", TicketStatus.PAID);

        assertThat(paid).extracting(Ticket::getTicketCode).containsExactlyInAnyOrder("TCK-0001", "TCK-0002");
    }

    @Test
    @DisplayName("AC-008 / FR-TKT-008: el conteo de ventas solo incluye tickets PAID")
    void countsOnlyPaidTickets() {
        assertThat(ticketRepository.countByEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).isEqualTo(2L);
        assertThat(ticketRepository.countByEventCodeAndStatus("CMF-2026", TicketStatus.RESERVED)).isEqualTo(1L);
        assertThat(ticketRepository.countByEventCodeAndStatus("NO-EXISTE", TicketStatus.PAID)).isZero();
    }
}
