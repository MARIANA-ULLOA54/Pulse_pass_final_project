package com.pulse.PulsePass.mapper;

import com.pulse.PulsePass.domain.Artist;
import com.pulse.PulsePass.dto.response.ArtistResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtistMapper {

    ArtistResponse toResponse(Artist artist);
}
