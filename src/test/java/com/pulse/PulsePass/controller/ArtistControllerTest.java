package com.pulse.PulsePass.controller;

import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    // TEST-CTRL-ART-001
    @Test
    void findById_existingArtist_returns200() throws Exception {
        when(artistService.findById(1L)).thenReturn(ControllerTestData.artist());

        mockMvc.perform(get("/api/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findById(1L);
    }

    // TEST-CTRL-ART-002
    @Test
    void findById_missingArtist_returns404() throws Exception {
        when(artistService.findById(99L)).thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(get("/api/artists/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    // TEST-CTRL-ART-003
    @Test
    void findByStageName_returns200() throws Exception {
        when(artistService.findByStageName("Solar Beat")).thenReturn(ControllerTestData.artist());

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    void findByStageName_missingArtist_returns404() throws Exception {
        when(artistService.findByStageName("Nadie")).thenThrow(new ResourceNotFoundException("Artist not found: Nadie"));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Nadie"))
                .andExpect(status().isNotFound());
    }

    // TEST-CTRL-ART-004
    @Test
    void findActiveArtists_returns200() throws Exception {
        when(artistService.findActiveArtists()).thenReturn(List.of(ControllerTestData.artist()));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(artistService).findActiveArtists();
    }
}
