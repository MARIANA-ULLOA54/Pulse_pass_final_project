package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.enums.EventStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // ---------- Query Methods (consultas simples) ----------

    /** FR-EVT-001 / AC-002: evento por codigo, trayendo su venue. */
    @EntityGraph(attributePaths = "venue")
    Optional<Event> findByEventCode(String eventCode);

    boolean existsByEventCode(String eventCode);

    /** FR-EVT-005 / UC-06: cartelera ordenada cronologicamente. */
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    /** FR-VEN-004: eventos de un venue navegando Event -> Venue.code. */
    List<Event> findByVenue_Code(String venueCode);

    // ---------- JPQL (consultas con JOIN) ----------

    /** FR-SRC-001 / FR-ART-004: eventos de un artista por stageName, sin duplicados. */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.artists a
            WHERE a.stageName = :stageName
            ORDER BY e.eventDate ASC
            """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    /** FR-SRC-002: eventos de una ciudad en los que participa un artista especifico. */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE v.city = :city
              AND a.stageName = :stageName
            ORDER BY e.eventDate ASC
            """)
    List<Event> findByCityAndArtistStageName(@Param("city") String city,
                                             @Param("stageName") String stageName);

    /**
     * FR-SRC-003 / UC-09: eventos recomendados. Publicados, posteriores a una fecha,
     * en una ciudad y con un artista cuyo nombre contenga el texto (case-insensitive).
     */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE e.status = :status
              AND e.eventDate > :after
              AND v.city = :city
              AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> findRecommendedEvents(@Param("status") EventStatus status,
                                      @Param("after") LocalDateTime after,
                                      @Param("city") String city,
                                      @Param("artistText") String artistText);
}
