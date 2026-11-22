package com.pulse.PulsePass.controller;

import com.pulse.PulsePass.domain.enums.EventCategory;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.dto.request.CreateEventRequest;
import com.pulse.PulsePass.exception.BusinessRuleException;
import com.pulse.PulsePass.exception.DuplicateResourceException;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.service.AuthorizationService;
import com.pulse.PulsePass.service.EventService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import com.pulse.PulsePass.security.JwtAuthenticationFilter;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoBean(types = JwtAuthenticationFilter.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private AuthorizationService authorizationService;

    // TEST-CTRL-EVT-001 / AC-CTRL003
    @Test
    void create_validRequest_returns201() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(ControllerTestData.event(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestData.VALID_EVENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        ArgumentCaptor<CreateEventRequest> captor = ArgumentCaptor.forClass(CreateEventRequest.class);
        verify(eventService).create(captor.capture());
        assertThat(captor.getValue().eventCode()).isEqualTo("CMF-2026");
        assertThat(captor.getValue().category()).isEqualTo(EventCategory.MUSIC);
        assertThat(captor.getValue().minimumAge()).isEqualTo(18);
    }

    // TEST-CTRL-EVT-002 / AC-CTRL004
    @Test
    void create_invalidRequest_returns400WithFieldDetailsAndDoesNotCallService() throws Exception {
        String invalid = """
                {"eventCode": "", "name": " ", "minimumAge": -1, "venueCode": ""}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.name").value("Name is required"))
                .andExpect(jsonPath("$.details.category").value("Category is required"))
                .andExpect(jsonPath("$.details.eventDate").value("Event date is required"))
                .andExpect(jsonPath("$.details.minimumAge").exists())
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any());
    }

    @Test
    void create_malformedJson_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        verify(eventService, never()).create(any());
    }

    @Test
    void create_duplicatedEventCode_returns409() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new DuplicateResourceException("Event already exists: CMF-2026"));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestData.VALID_EVENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // TEST-CTRL-EVT-003
    @Test
    void findByCode_existingEvent_returns200() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(ControllerTestData.event(EventStatus.DRAFT));

        mockMvc.perform(get("/api/events/CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).findByCode("CMF-2026");
    }

    // TEST-CTRL-EVT-004
    @Test
    void findByCode_missingEvent_returns404() throws Exception {
        when(eventService.findByCode("CMF-2026"))
                .thenThrow(new ResourceNotFoundException("Event not found: CMF-2026"));

        mockMvc.perform(get("/api/events/CMF-2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: CMF-2026"));
    }

    // TEST-CTRL-EVT-005
    @Test
    void findPublishedEvents_returns200() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(ControllerTestData.eventSummary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
    }

    // TEST-CTRL-EVT-006 / AC-CTRL005
    @Test
    void publish_validTransition_returns200() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(ControllerTestData.event(EventStatus.PUBLISHED));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-007 / AC-CTRL006
    @Test
    void publish_businessRuleViolation_returns409() throws Exception {
        when(eventService.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException("Only DRAFT events can be published. Current status: CANCELLED"));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only DRAFT events can be published. Current status: CANCELLED"));
    }

    // TEST-CTRL-EVT-008
    @Test
    void addArtist_validAssociation_returns200() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(ControllerTestData.event(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events/CMF-2026/artists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artists.length()").value(1));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    void addArtist_duplicatedAssociation_returns409() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenThrow(new DuplicateResourceException("Artist already associated"));

        mockMvc.perform(post("/api/events/CMF-2026/artists/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void addArtist_nonNumericArtistId_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/events/CMF-2026/artists/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.artistId").exists());

        verify(eventService, never()).addArtist(any(), any());
    }

    // TEST-CTRL-EVT-009
    @Test
    void findByArtist_returns200() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(ControllerTestData.eventSummary()));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }

    @Test
    void findByArtist_missingParameter_returns400() throws Exception {
        mockMvc.perform(get("/api/events/by-artist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.stageName").exists());

        verify(eventService, never()).findByArtist(any());
    }
}
