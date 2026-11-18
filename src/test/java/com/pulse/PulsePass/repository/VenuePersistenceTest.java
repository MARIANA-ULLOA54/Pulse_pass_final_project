package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.support.PostgresDataJpaTest;
import com.pulse.PulsePass.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-VEN-001..003, UC-01, AC-001, QT-009. */
@PostgresDataJpaTest
class VenuePersistenceTest {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    @DisplayName("AC-001: un venue valido se recupera por ID y por codigo")
    void persistsAndRetrievesVenue() {
        Venue saved = venueRepository.saveAndFlush(TestData.marinaConventionCenter());

        assertThat(venueRepository.findById(saved.getId())).isPresent();
        Venue found = venueRepository.findByCode("VEN-SMR-01").orElseThrow();
        assertThat(found.getName()).isEqualTo("Marina Convention Center");
        assertThat(found.getCity()).isEqualTo("Santa Marta");
        assertThat(found.getCapacity()).isGreaterThan(0).isEqualTo(5000);
        assertThat(found.isActive()).isTrue();
    }

    @Test
    @DisplayName("FR-VEN-002: PostgreSQL rechaza un codigo de venue duplicado")
    void rejectsDuplicateCode() {
        venueRepository.saveAndFlush(TestData.venue("VEN-DUP-01", "Santa Marta", 100));

        assertThatThrownBy(() -> venueRepository.saveAndFlush(TestData.venue("VEN-DUP-01", "Bogota", 200)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-VEN-003: PostgreSQL rechaza capacidad igual a cero")
    void rejectsZeroCapacity() {
        assertThatThrownBy(() -> venueRepository.saveAndFlush(TestData.venue("VEN-ZERO", "Santa Marta", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-VEN-003: PostgreSQL rechaza capacidad negativa")
    void rejectsNegativeCapacity() {
        assertThatThrownBy(() -> venueRepository.saveAndFlush(TestData.venue("VEN-NEG", "Santa Marta", -10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("existsByCode distingue venues existentes de inexistentes")
    void existsByCode() {
        venueRepository.saveAndFlush(TestData.venue("VEN-EX-01", "Santa Marta", 50));

        assertThat(venueRepository.existsByCode("VEN-EX-01")).isTrue();
        assertThat(venueRepository.existsByCode("VEN-NO-EXISTE")).isFalse();
    }
}
