package com.pulse.PulsePass.support;

import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.Ticket;
import com.pulse.PulsePass.domain.User;
import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.domain.enums.EventCategory;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.domain.enums.TicketStatus;
import com.pulse.PulsePass.domain.enums.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fabricas de datos de prueba basadas en el escenario de la seccion 16 del PRD. */
public final class TestData {

    public static final LocalDateTime FUTURE = LocalDateTime.of(2030, 6, 1, 20, 0);

    private TestData() {
    }

    public static Venue venue(String code, String city, int capacity) {
        return new Venue(code, "Venue " + code, city, "Calle 1 # 2-3", capacity, true);
    }

    public static Venue marinaConventionCenter() {
        return new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Marina de Santa Marta", 5000, true);
    }

    public static Event event(String code, Venue venue, EventStatus status, LocalDateTime date) {
        return new Event(code, "Evento " + code, "Descripcion de " + code,
                EventCategory.MUSIC, status, date, 0, venue);
    }

    public static User user(String username, String email) {
        return new User(username, email, true);
    }

    public static Ticket ticket(String code, User user, Event event, TicketType type,
                                TicketStatus status, String price) {
        return new Ticket(code, type, new BigDecimal(price), status, LocalDateTime.of(2026, 11, 1, 10, 0), user, event);
    }
}
