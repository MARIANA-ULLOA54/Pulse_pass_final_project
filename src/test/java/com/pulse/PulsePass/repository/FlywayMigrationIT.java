package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.support.PostgresDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** QT-001 y QT-002: Flyway aplica V1 a V4 y Hibernate valida el esquema. */
@PostgresDataJpaTest
class FlywayMigrationIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("QT-001: Flyway aplica V1 a V4 en orden desde una base vacia")
    void appliesAllMigrationsInOrder() {
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE AND version IS NOT NULL ORDER BY installed_rank",
                String.class);

        assertThat(versions).containsExactly("1", "2", "3", "4");
    }

    @Test
    @DisplayName("V1 crea las siete tablas del modelo")
    void createsExpectedTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables).contains("venues", "events", "artists", "event_artists",
                "users", "user_profiles", "tickets");
    }

    @Test
    @DisplayName("V2 inserta el catalogo inicial de cinco artistas")
    void seedsInitialArtists() {
        List<String> names = jdbc.queryForList("SELECT stage_name FROM artists", String.class);

        assertThat(names).contains("Solar Beat", "Neon Waves", "Caribbean Sound", "Ocean Drive", "Digital Pulse");
    }

    @Test
    @DisplayName("V3 agrega streaming_url VARCHAR(500) nullable a events")
    void addsNullableStreamingUrl() {
        String nullable = jdbc.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'events' AND column_name = 'streaming_url'",
                String.class);
        Integer length = jdbc.queryForObject(
                "SELECT character_maximum_length FROM information_schema.columns "
                        + "WHERE table_name = 'events' AND column_name = 'streaming_url'",
                Integer.class);

        assertThat(nullable).isEqualTo("YES");
        assertThat(length).isEqualTo(500);
    }
}
