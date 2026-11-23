package com.pulse.PulsePass.controller;

import com.pulse.PulsePass.domain.enums.TicketStatus;
import com.pulse.PulsePass.domain.enums.TicketType;
import com.pulse.PulsePass.dto.request.PurchaseTicketRequest;
import com.pulse.PulsePass.exception.BusinessRuleException;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.service.TicketService;
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

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoBean(types = JwtAuthenticationFilter.class)
class TicketControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TicketService ticketService;

    // TEST-CTRL-TKT-001 / AC-CTRL009
    @Test
    void purchase_validRequest_returns201WithPaidTicket() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(ControllerTestData.ticket(TicketStatus.PAID));

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ControllerTestData.VALID_PURCHASE_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.price").value(125000.00))
                .andExpect(jsonPath("$.userEmail").value("andrea@email.com"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        ArgumentCaptor<PurchaseTicketRequest> captor =
                ArgumentCaptor.forClass(PurchaseTicketRequest.class);
        verify(ticketService).purchase(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(TicketType.VIP);
    }

    // TEST-CTRL-TKT-002
    @Test
    void purchase_invalidRequest_returns400AndDoesNotCallService() throws Exception {
        String invalid =
                """
                {"userEmail": "not-an-email", "eventCode": ""}
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.userEmail").exists())
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    void purchase_unknownTicketType_returns400AndDoesNotCallService() throws Exception {
        String unknownType = ControllerTestData.VALID_PURCHASE_JSON.replace("VIP", "PLATINUM");

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(unknownType))
                .andExpect(status().isBadRequest());

        verify(ticketService, never()).purchase(any());
    }

    // TEST-CTRL-TKT-003
    @Test
    void purchase_missingUser_returns404() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@email.com"));

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ControllerTestData.VALID_PURCHASE_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: andrea@email.com"));
    }

    // TEST-CTRL-TKT-004 / AC-CTRL010
    @Test
    void purchase_businessRuleViolation_returns409() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet minimum age."));

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ControllerTestData.VALID_PURCHASE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet minimum age."));
    }

    // TEST-CTRL-TKT-005
    @Test
    void findByCode_returns200() throws Exception {
        when(ticketService.findByCode("TKT-0001"))
                .thenReturn(ControllerTestData.ticket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TKT-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"));

        verify(ticketService).findByCode("TKT-0001");
    }

    @Test
    void findByCode_missingTicket_returns404() throws Exception {
        when(ticketService.findByCode("NOPE"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: NOPE"));

        mockMvc.perform(get("/api/tickets/NOPE")).andExpect(status().isNotFound());
    }

    // TEST-CTRL-TKT-006
    @Test
    void findByUser_returns200() throws Exception {
        when(ticketService.findByUserEmail("andrea@email.com"))
                .thenReturn(List.of(ControllerTestData.ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@email.com"));

        verify(ticketService).findByUserEmail("andrea@email.com");
    }

    // TEST-CTRL-TKT-007
    @Test
    void findPaidTicketsByEvent_returns200() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026"))
                .thenReturn(List.of(ControllerTestData.ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    @Test
    void findPaidTicketsByEvent_missingEvent_returns404() throws Exception {
        when(ticketService.findPaidTicketsByEvent("NOPE"))
                .thenThrow(new ResourceNotFoundException("Event not found: NOPE"));

        mockMvc.perform(get("/api/events/NOPE/tickets/paid")).andExpect(status().isNotFound());
    }

    // TEST-CTRL-TKT-008 / AC-CTRL011
    @Test
    void cancel_paidTicket_returns200() throws Exception {
        when(ticketService.cancel("TKT-0001"))
                .thenReturn(ControllerTestData.ticket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TKT-0001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TKT-0001");
    }

    // TEST-CTRL-TKT-009 / AC-CTRL012
    @Test
    void cancel_usedTicket_returns409() throws Exception {
        when(ticketService.cancel("TKT-0001"))
                .thenThrow(
                        new BusinessRuleException(
                                "Only PAID tickets can be cancelled. Current status: USED"));

        mockMvc.perform(patch("/api/tickets/TKT-0001/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // TEST-CTRL-TKT-010 / AC-CTRL013
    @Test
    void markAsUsed_paidTicket_returns200() throws Exception {
        when(ticketService.markAsUsed("TKT-0001"))
                .thenReturn(ControllerTestData.ticket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TKT-0001/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TKT-0001");
    }

    // TEST-CTRL-TKT-011 / AC-CTRL014
    @Test
    void markAsUsed_cancelledTicket_returns409() throws Exception {
        when(ticketService.markAsUsed("TKT-0001"))
                .thenThrow(
                        new BusinessRuleException("A CANCELLED ticket cannot be used: TKT-0001"));

        mockMvc.perform(patch("/api/tickets/TKT-0001/use"))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message").value("A CANCELLED ticket cannot be used: TKT-0001"));
    }
}
