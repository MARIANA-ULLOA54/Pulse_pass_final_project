package com.pulse.PulsePass.service;

import com.pulse.PulsePass.domain.Artist;
import com.pulse.PulsePass.dto.response.ArtistResponse;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.mapper.ArtistMapper;
import com.pulse.PulsePass.repository.ArtistRepository;
import com.pulse.PulsePass.service.impl.ArtistServiceImpl;
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
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ArtistMapper artistMapper;
    @InjectMocks
    private ArtistServiceImpl service;

    private final ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Colombia", "Pop", true);

    @Test
    void findById_existingArtist_returnsDto() {
        Artist artist = ServiceTestData.artist("Solar Beat");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        assertThat(service.findById(1L)).isEqualTo(response);
    }

    @Test
    void findById_missingArtist_throwsResourceNotFound() {
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByStageName_missingArtist_throwsResourceNotFound() {
        when(artistRepository.findByStageNameIgnoreCase("Nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByStageName("Nadie"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nadie");
    }

    @Test
    void findActiveArtists_returnsMappedActiveArtists() {
        Artist artist = ServiceTestData.artist("Solar Beat");
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        assertThat(service.findActiveArtists()).containsExactly(response);
    }
}
