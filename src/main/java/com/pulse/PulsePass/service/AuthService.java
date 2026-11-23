package com.pulse.PulsePass.service;

import com.pulse.PulsePass.dto.request.*;
import com.pulse.PulsePass.dto.response.*;
import com.pulse.PulsePass.repository.*;
import com.pulse.PulsePass.domain.*;
import com.pulse.PulsePass.security.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final UserService userService;
    private final UserRepository users;
    private final UserCredentialRepository credentials;
    private final UserRoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuthenticationManager manager;
    private final JwtService jwt;

    public AuthService(
            UserService userService,
            UserRepository users,
            UserCredentialRepository credentials,
            UserRoleRepository roles,
            PasswordEncoder encoder,
            AuthenticationManager manager,
            JwtService jwt) {
        this.userService = userService;
        this.users = users;
        this.credentials = credentials;
        this.roles = roles;
        this.encoder = encoder;
        this.manager = manager;
        this.jwt = jwt;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (request.password() == null
                || request.password().length() < 12
                || request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters and at most 72 UTF-8 bytes");
        UserResponse response = userService.register(request);
        User user = users.findByUsername(request.username()).orElseThrow();
        credentials.save(new UserCredential(user, encoder.encode(request.password())));
        roles.save(new UserRole(user.getId(), "USER"));
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        var auth =
                manager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.identifier(), request.password()));
        SecurityPrincipal principal = (SecurityPrincipal) auth.getPrincipal();
        return new AuthResponse(jwt.issue(principal.userId()), "Bearer", jwt.expiresIn());
    }
}
