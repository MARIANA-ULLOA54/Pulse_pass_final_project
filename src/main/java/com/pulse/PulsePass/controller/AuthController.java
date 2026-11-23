package com.pulse.PulsePass.controller;

import com.pulse.PulsePass.config.OpenApiConfig;
import com.pulse.PulsePass.dto.request.LoginRequest;
import com.pulse.PulsePass.dto.response.*;
import com.pulse.PulsePass.security.SecurityPrincipal;
import com.pulse.PulsePass.service.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(auth.login(request));
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityPrincipal principal) {
        return ResponseEntity.ok(
                new AuthenticatedUserResponse(
                        principal.userId(),
                        principal.username(),
                        principal.email(),
                        principal.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()));
    }
}
