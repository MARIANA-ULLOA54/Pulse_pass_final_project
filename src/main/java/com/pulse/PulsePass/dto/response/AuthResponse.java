package com.pulse.PulsePass.dto.response;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {}
