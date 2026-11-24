package com.pulse.PulsePass.controller;

import com.pulse.PulsePass.domain.enums.EventCategory;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.domain.enums.TicketStatus;
import com.pulse.PulsePass.domain.enums.TicketType;
import com.pulse.PulsePass.dto.response.ArtistResponse;
import com.pulse.PulsePass.dto.response.EventResponse;
import com.pulse.PulsePass.dto.response.EventSummaryResponse;
import com.pulse.PulsePass.dto.response.TicketResponse;
import com.pulse.PulsePass.dto.response.UserResponse;
import com.pulse.PulsePass.dto.response.VenueResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Escenario de referencia del PRD (seccion 23) para los tests de Controller. */
final class ControllerTestData {

    static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 12, 20, 20, 0);

    static final String VALID_EVENT_JSON =
            """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival",
              "category": "MUSIC",
              "eventDate": "2026-12-20T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    static final String VALID_USER_JSON =
            """
            {
              "username": "andrea",
              "email": "andrea@email.com",
              "firstName": "Andrea",
              "lastName": "Lopez",
              "phone": "3001234567",
              "city": "Santa Marta",
              "birthDate": "2001-05-10",
              "password": "password-for-test"
            }
            """;

    static final String VALID_PURCHASE_JSON =
            """
            {
              "userEmail": "andrea@email.com",
              "eventCode": "CMF-2026",
              "type": "VIP"
            }
            """;

    private ControllerTestData() {}

    static VenueResponse venue() {
        return new VenueResponse(
                1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", 3, true);
    }

    static ArtistResponse artist() {
        return new ArtistResponse(1L, "Solar Beat", "Colombia", "Pop", true);
    }

    static EventResponse event(EventStatus status) {
        return new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival",
                EventCategory.MUSIC,
                status,
                EVENT_DATE,
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of(artist()));
    }

    static EventSummaryResponse eventSummary() {
        return new EventSummaryResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                EVENT_DATE,
                "VEN-SMR-01",
                "Marina Convention Center");
    }

    static UserResponse user() {
        return new UserResponse(
                1L,
                "andrea",
                "andrea@email.com",
                true,
                "Andrea",
                "Lopez",
                "3001234567",
                "Santa Marta",
                LocalDate.of(2001, 5, 10));
    }

    static TicketResponse ticket(TicketStatus status) {
        return new TicketResponse(
                1L,
                "TKT-0001",
                TicketType.VIP,
                new BigDecimal("125000.00"),
                status,
                LocalDateTime.of(2026, 11, 18, 9, 0),
                "andrea@email.com",
                "CMF-2026",
                "Caribbean Music Fest 2026");
    }
}
