package com.pulse.PulsePass.mapper;

import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.dto.response.VenueResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    VenueResponse toResponse(Venue venue);
}
