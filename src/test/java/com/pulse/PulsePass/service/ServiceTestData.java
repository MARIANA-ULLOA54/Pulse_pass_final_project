package com.pulse.PulsePass.service;

import com.pulse.PulsePass.domain.*;
import com.pulse.PulsePass.domain.enums.EventCategory;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.domain.enums.TicketStatus;
import com.pulse.PulsePass.domain.enums.TicketType;
import com.pulse.PulsePass.dto.response.TicketResponse;

import java.math.BigDecimal;
import java.time.*;

/** Datos del escenario de aceptacion (PRD seccion 47) para los unit tests de Service. */
final class ServiceTestData {

    /** "Hoy" fijo: 2026-11-18T10:00. */
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC);

    static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 12, 20, 20, 0);

    private ServiceTestData() {}

    static Venue venue(boolean active) {
        return new Venue(
                "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", 3, active);
    }

    static Event event(EventStatus status, Venue venue) {
        return new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival",
                EventCategory.MUSIC,
                status,
                EVENT_DATE,
                18,
                venue);
    }

    static Artist artist(String stageName) {
        return new Artist(stageName, "Colombia", "Pop", true);
    }

    /** Usuario con perfil cuya fecha de nacimiento da la edad indicada el dia del evento. */
    static User user(String username, String email, int ageAtEvent, boolean active) {
        User user = new User(username, email, active);
        LocalDate birthDate = EVENT_DATE.toLocalDate().minusYears(ageAtEvent).minusDays(30);
        user.setProfile(new UserProfile(username, "Test", "3000000000", "Santa Marta", birthDate));
        return user;
    }

    static Ticket ticket(TicketStatus status, User user, Event event) {
        return new Ticket(
                "TKT-0001",
                TicketType.GENERAL,
                new BigDecimal("50000.00"),
                status,
                LocalDateTime.of(2026, 11, 18, 9, 0),
                user,
                event);
    }

    static TicketResponse toResponse(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getTicketCode(),
                t.getType(),
                t.getPrice(),
                t.getStatus(),
                t.getPurchaseDate(),
                t.getUser().getEmail(),
                t.getEvent().getEventCode(),
                t.getEvent().getName());
    }
}
