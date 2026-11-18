package com.pulse.PulsePass.dto.request;

import com.pulse.PulsePass.domain.enums.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** El precio no viaja en el request: lo calcula el sistema segun el tipo de ticket. */
public record PurchaseTicketRequest(
        @NotBlank(message = "User email is required")
        @Email(message = "User email must be valid")
        String userEmail,

        @NotBlank(message = "Event code is required")
        String eventCode,

        @NotNull(message = "Ticket type is required")
        TicketType type
) {}
