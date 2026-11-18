package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Ticket;
import com.pulse.PulsePass.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** FR-TKT-006: tickets de un usuario navegando Ticket -> User -> email. */
    List<Ticket> findByUser_Email(String email);

    /** FR-TKT-006: tickets de un usuario por email y estado. */
    List<Ticket> findByUser_EmailAndStatus(String email, TicketStatus status);

    /**
     * FR-TKT-007: tickets de un evento por eventCode y estado. Se usa Query Method porque
     * es un filtro simple sobre una relacion; basta navegar Ticket -> Event.eventCode.
     */
    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

    /** FR-TKT-008: conteo de tickets de un evento en un estado dado (PAID para ventas). */
    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            JOIN t.event e
            WHERE e.eventCode = :eventCode
              AND t.status = :status
            """)
    long countByEventCodeAndStatus(@Param("eventCode") String eventCode,
                                   @Param("status") TicketStatus status);

    /** FR-SRC-004: tickets cuyo evento es posterior a una fecha, en orden cronologico. */
    @Query("""
            SELECT t
            FROM Ticket t
            JOIN t.event e
            WHERE e.eventDate > :after
            ORDER BY e.eventDate ASC, t.id ASC
            """)
    List<Ticket> findTicketsOfEventsAfter(@Param("after") LocalDateTime after);
}
