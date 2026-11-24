package com.pulse.PulsePass.controller;

import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pulse.PulsePass.security.JwtAuthenticationFilter;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

@WebMvcTest(VenueController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoBean(types = JwtAuthenticationFilter.class)
class VenueControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private VenueService venueService;

    // TEST-CTRL-VEN-001 / AC-CTRL001
    @Test
    void findByCode_existingVenue_returns200() throws Exception {
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(ControllerTestData.venue());

        mockMvc.perform(get("/api/venues/VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.capacity").value(3));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    // TEST-CTRL-VEN-002 / AC-CTRL002
    @Test
    void findByCode_missingVenue_returns404WithErrorResponse() throws Exception {
        when(venueService.findByCode("NOPE"))
                .thenThrow(new ResourceNotFoundException("Venue not found: NOPE"));

        mockMvc.perform(get("/api/venues/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: NOPE"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // TEST-CTRL-VEN-003
    @Test
    void findActiveVenues_returns200WithList() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of(ControllerTestData.venue()));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(venueService).findActiveVenues();
    }

    // Error inesperado -> 500 sin detalles internos (NFR-CTRL-007)
    @Test
    void unexpectedError_returns500WithoutInternalDetails() throws Exception {
        when(venueService.findActiveVenues())
                .thenThrow(new IllegalStateException("db password leaked"));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void unknownPath_returns404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
