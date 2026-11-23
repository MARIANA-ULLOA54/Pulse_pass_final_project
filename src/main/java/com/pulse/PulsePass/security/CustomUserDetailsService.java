package com.pulse.PulsePass.security;

import com.pulse.PulsePass.domain.User;
import com.pulse.PulsePass.repository.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final UserCredentialRepository credentials;
    private final UserRoleRepository roles;

    public CustomUserDetailsService(UserRepository u, UserCredentialRepository c, UserRoleRepository r) {
        users = u;
        credentials = c;
        roles = r;
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityPrincipal loadUserByUsername(String identifier) {
        User u = users.findByUsername(identifier)
                .or(() -> users.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return load(u);
    }

    @Transactional(readOnly = true)
    public SecurityPrincipal loadById(Long id) {
        return load(users.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials")));
    }

    private SecurityPrincipal load(User u) {
        var credential = credentials.findById(u.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        var authorities = roles.findByUserId(u.getId())
                .stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRole()))
                .toList();
        return new SecurityPrincipal(
                u.getId(), u.getUsername(), u.getEmail(), credential.getPasswordHash(), u.isActive(), authorities);
    }
}
