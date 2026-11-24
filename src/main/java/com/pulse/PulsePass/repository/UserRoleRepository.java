package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.Key> {
    List<UserRole> findByUserId(Long userId);
}
