package com.pulse.PulsePass.dto.request;

import com.pulse.PulsePass.domain.enums.TicketType;

/** El precio no viaja en el request: lo calcula el sistema segun el tipo de ticket. */
public record PurchaseTicketRequest(
        String userEmail,
        String eventCode,
        TicketType type
) {}
