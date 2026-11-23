package com.pulse.PulsePass.security;

import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public record SecurityPrincipal(
        Long userId,
        String username,
        String email,
        String password,
        boolean active,
        Collection<? extends GrantedAuthority> authorities) implements UserDetails {

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEnabled() {
        return active;
    }
}
