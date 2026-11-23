package com.pulse.PulsePass.service.pricing;

import com.pulse.PulsePass.domain.enums.TicketType;
import com.pulse.PulsePass.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estrategia de precios encapsulada (BR-TICKET-009): precio base por multiplicador segun el tipo.
 * GENERAL = base, STUDENT = descuento, VIP y BACKSTAGE = recargo.
 */
@Component
public class TicketPriceCalculator {

    public static final BigDecimal BASE_PRICE = new BigDecimal("50000.00");

    public BigDecimal calculate(TicketType type) {
        if (type == null) {
            throw new BusinessRuleException("Ticket type is required.");
        }
        BigDecimal price =
                BASE_PRICE.multiply(multiplierOf(type)).setScale(2, RoundingMode.HALF_UP);
        if (price.signum() < 0) {
            throw new BusinessRuleException("Ticket price cannot be negative.");
        }
        return price;
    }

    private BigDecimal multiplierOf(TicketType type) {
        return switch (type) {
            case GENERAL -> new BigDecimal("1.00");
            case STUDENT -> new BigDecimal("0.50");
            case VIP -> new BigDecimal("2.50");
            case BACKSTAGE -> new BigDecimal("4.00");
        };
    }
}
