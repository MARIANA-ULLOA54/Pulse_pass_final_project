package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /** FR-ART-001: recuperar un artista por su nombre artistico. */
    Optional<Artist> findByStageName(String stageName);

    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    List<Artist> findByActiveTrueOrderByStageNameAsc();
}
