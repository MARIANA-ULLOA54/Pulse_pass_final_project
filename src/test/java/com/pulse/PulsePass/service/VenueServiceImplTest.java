package com.pulse.PulsePass.service;

import com.pulse.PulsePass.domain.Venue;
import com.pulse.PulsePass.dto.response.VenueResponse;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.mapper.VenueMapper;
import com.pulse.PulsePass.repository.VenueRepository;
import com.pulse.PulsePass.service.impl.VenueServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock private VenueRepository venueRepository;
    @Mock private VenueMapper venueMapper;
    @InjectMocks private VenueServiceImpl service;

    private final VenueResponse response =
            new VenueResponse(
                    1L,
                    "VEN-SMR-01",
                    "Marina Convention Center",
                    "Santa Marta",
                    "Calle 1",
                    3,
                    true);

    @Test
    void findByCode_existingVenue_returnsDto() {
        Venue venue = ServiceTestData.venue(true);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(response);

        VenueResponse result = service.findByCode("VEN-SMR-01");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findByCode_missingVenue_throwsResourceNotFound() {
        when(venueRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NOPE");
    }

    @Test
    void findActiveVenues_returnsMappedActiveVenues() {
        Venue venue = ServiceTestData.venue(true);
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(response);

        assertThat(service.findActiveVenues()).containsExactly(response);
    }
}
