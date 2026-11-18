package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    /** FR-VEN-001: recuperar un venue por su codigo de negocio. */
    Optional<Venue> findByCode(String code);

    boolean existsByCode(String code);
}
