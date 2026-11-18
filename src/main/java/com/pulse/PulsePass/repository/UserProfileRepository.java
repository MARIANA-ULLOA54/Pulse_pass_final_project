package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /** FR-USR-004: perfil de un usuario navegando la relacion User 1:1 UserProfile. */
    Optional<UserProfile> findByUser_Id(Long userId);
}
