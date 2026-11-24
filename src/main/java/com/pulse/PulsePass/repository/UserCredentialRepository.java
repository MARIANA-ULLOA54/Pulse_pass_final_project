package com.pulse.PulsePass.repository;

import com.pulse.PulsePass.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {}
