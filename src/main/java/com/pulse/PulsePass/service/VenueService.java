package com.pulse.PulsePass.service;

import com.pulse.PulsePass.dto.response.VenueResponse;

import java.util.List;

public interface VenueService {

    VenueResponse findByCode(String code);

    List<VenueResponse> findActiveVenues();
}
