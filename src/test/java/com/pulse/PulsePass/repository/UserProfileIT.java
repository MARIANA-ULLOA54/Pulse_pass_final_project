package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.User;
import com.pulse.PulsePass.domain.UserProfile;
import com.pulse.PulsePass.support.PostgresDataJpaTest;
import com.pulse.PulsePass.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-USR-001..004, UC-04, QT-004, AC-004. */
@PostgresDataJpaTest
class UserProfileIT {

    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private EntityManager entityManager;

    private UserProfile profileFor(User user, String firstName) {
        UserProfile profile =
                new UserProfile(
                        firstName, "Gomez", "3001234567", "Santa Marta", LocalDate.of(2000, 5, 20));
        profile.setUser(user);
        return profile;
    }

    @Test
    @DisplayName(
            "FR-USR-001: un usuario se persiste y se recupera por email sin importar mayusculas")
    void persistsUserAndFindsByEmailIgnoringCase() {
        userRepository.saveAndFlush(TestData.user("andrea", "Andrea@Pulse.com"));
        entityManager.clear();

        User found = userRepository.findByEmailIgnoreCase("andrea@pulse.com").orElseThrow();

        assertThat(found.getUsername()).isEqualTo("andrea");
        assertThat(userRepository.findByUsername("andrea")).isPresent();
    }

    @Test
    @DisplayName("FR-USR-002: PostgreSQL rechaza username duplicado")
    void rejectsDuplicateUsername() {
        userRepository.saveAndFlush(TestData.user("carlos", "carlos@pulse.com"));

        assertThatThrownBy(
                        () ->
                                userRepository.saveAndFlush(
                                        TestData.user("carlos", "otro@pulse.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-USR-002: PostgreSQL rechaza email duplicado")
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(TestData.user("laura", "laura@pulse.com"));

        assertThatThrownBy(
                        () ->
                                userRepository.saveAndFlush(
                                        TestData.user("laura2", "laura@pulse.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName(
            "FR-USR-004 / QT-004: el perfil se recupera desde la relacion User 1:1 UserProfile")
    void retrievesProfileThroughOneToOne() {
        User user = userRepository.saveAndFlush(TestData.user("miguel", "miguel@pulse.com"));
        userProfileRepository.saveAndFlush(profileFor(user, "Miguel"));
        entityManager.clear();

        UserProfile profile = userProfileRepository.findByUser_Id(user.getId()).orElseThrow();

        assertThat(profile.getFirstName()).isEqualTo("Miguel");
        assertThat(profile.getLastName()).isEqualTo("Gomez");
        assertThat(profile.getCity()).isEqualTo("Santa Marta");
        assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2000, 5, 20));
        assertThat(profile.getUser().getEmail()).isEqualTo("miguel@pulse.com");
    }

    @Test
    @DisplayName("AC-004: la base de datos impide un segundo perfil para el mismo usuario")
    void rejectsSecondProfileForSameUser() {
        User user = userRepository.saveAndFlush(TestData.user("sofia", "sofia@pulse.com"));
        userProfileRepository.saveAndFlush(profileFor(user, "Sofia"));

        assertThatThrownBy(
                        () -> userProfileRepository.saveAndFlush(profileFor(user, "Sofia Segunda")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
