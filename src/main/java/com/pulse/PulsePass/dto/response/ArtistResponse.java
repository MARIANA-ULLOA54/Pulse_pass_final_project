package com.pulse.PulsePass.dto.response;

public record ArtistResponse(
        Long id, String stageName, String country, String genre, boolean active) {}
