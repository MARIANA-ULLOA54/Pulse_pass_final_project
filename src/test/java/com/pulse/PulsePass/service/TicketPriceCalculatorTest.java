package com.pulse.PulsePass.service;

import com.pulse.PulsePass.domain.enums.TicketType;
import com.pulse.PulsePass.exception.BusinessRuleException;
import com.pulse.PulsePass.service.pricing.TicketPriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator();

    @Test
    void calculate_general_returnsBasePrice() {
        assertThat(calculator.calculate(TicketType.GENERAL)).isEqualByComparingTo("50000.00");
    }

    @Test
    void calculate_student_appliesDiscount() {
        assertThat(calculator.calculate(TicketType.STUDENT)).isEqualByComparingTo("25000.00");
    }

    @Test
    void calculate_vipAndBackstage_applyIncreasingMultipliers() {
        BigDecimal vip = calculator.calculate(TicketType.VIP);
        BigDecimal backstage = calculator.calculate(TicketType.BACKSTAGE);

        assertThat(vip).isEqualByComparingTo("125000.00");
        assertThat(backstage).isEqualByComparingTo("200000.00").isGreaterThan(vip);
    }

    @Test
    void calculate_neverReturnsNegativePrice() {
        for (TicketType type : TicketType.values()) {
            assertThat(calculator.calculate(type).signum()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void calculate_nullType_throwsBusinessRule() {
        assertThatThrownBy(() -> calculator.calculate(null)).isInstanceOf(BusinessRuleException.class);
    }
}
