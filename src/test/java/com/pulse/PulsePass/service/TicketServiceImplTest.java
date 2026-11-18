package com.pulse.PulsePass.service;

import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.Ticket;
import com.pulse.PulsePass.domain.User;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.domain.enums.TicketStatus;
import com.pulse.PulsePass.domain.enums.TicketType;
import com.pulse.PulsePass.dto.request.PurchaseTicketRequest;
import com.pulse.PulsePass.dto.response.TicketResponse;
import com.pulse.PulsePass.exception.BusinessRuleException;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.mapper.TicketMapper;
import com.pulse.PulsePass.repository.EventRepository;
import com.pulse.PulsePass.repository.TicketRepository;
import com.pulse.PulsePass.repository.UserRepository;
import com.pulse.PulsePass.service.impl.TicketServiceImpl;
import com.pulse.PulsePass.service.pricing.TicketPriceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final String EVENT = "CMF-2026";

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketMapper ticketMapper;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(ticketRepository, userRepository, eventRepository, ticketMapper,
                new TicketPriceCalculator(), ServiceTestData.CLOCK);
    }

    private PurchaseTicketRequest purchaseBy(User user, TicketType type) {
        return new PurchaseTicketRequest(user.getEmail(), EVENT, type);
    }

    private User andrea() {
        return ServiceTestData.user("andrea", "andrea@email.com", 25, true);
    }

    private Event publishedEvent() {
        return ServiceTestData.event(EventStatus.PUBLISHED, ServiceTestData.venue(true));
    }

    private void stubUser(User user) {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
    }

    private void stubEvent(Event event) {
        when(eventRepository.findByEventCode(EVENT)).thenReturn(Optional.of(event));
    }

    private void stubPaidCount(long count) {
        when(ticketRepository.countByEventCodeAndStatus(EVENT, TicketStatus.PAID)).thenReturn(count);
    }

    private void stubSaveAndMapper() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenAnswer(inv -> ServiceTestData.toResponse(inv.getArgument(0)));
    }

    // TEST-TICKET-001 / AC-004
    @Test
    void purchase_validRequest_createsPaidTicketWithSystemPrice() {
        User user = andrea();
        Event event = publishedEvent();
        stubUser(user);
        stubEvent(event);
        stubPaidCount(0);
        stubSaveAndMapper();

        TicketResponse result = service.purchase(purchaseBy(user, TicketType.VIP));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo(new BigDecimal("125000.00"));
        assertThat(result.status()).isEqualTo(TicketStatus.PAID);
        assertThat(result.userEmail()).isEqualTo("andrea@email.com");
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any());
    }

    // TEST-TICKET-002
    @Test
    void purchase_missingUser_throwsResourceNotFound() {
        when(userRepository.findByEmailIgnoreCase("ghost@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.purchase(new PurchaseTicketRequest("ghost@email.com", EVENT, TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-003 / AC-007
    @Test
    void purchase_inactiveUser_throwsBusinessRule() {
        User miguel = ServiceTestData.user("miguel", "miguel@email.com", 30, false);
        stubUser(miguel);

        assertThatThrownBy(() -> service.purchase(purchaseBy(miguel, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_missingEvent_throwsResourceNotFound() {
        User user = andrea();
        stubUser(user);
        when(eventRepository.findByEventCode(EVENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.purchase(purchaseBy(user, TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-004
    @Test
    void purchase_draftEvent_throwsBusinessRule() {
        User user = andrea();
        stubUser(user);
        stubEvent(ServiceTestData.event(EventStatus.DRAFT, ServiceTestData.venue(true)));

        assertThatThrownBy(() -> service.purchase(purchaseBy(user, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-005
    @Test
    void purchase_cancelledEvent_throwsBusinessRule() {
        User user = andrea();
        stubUser(user);
        stubEvent(ServiceTestData.event(EventStatus.CANCELLED, ServiceTestData.venue(true)));

        assertThatThrownBy(() -> service.purchase(purchaseBy(user, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // BR-TICKET-005
    @Test
    void purchase_pastEvent_throwsBusinessRule() {
        User user = andrea();
        Event event = publishedEvent();
        event.setEventDate(LocalDateTime.of(2026, 1, 1, 20, 0));
        stubUser(user);
        stubEvent(event);

        assertThatThrownBy(() -> service.purchase(purchaseBy(user, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-006 / AC-006
    @Test
    void purchase_underageUser_throwsBusinessRule() {
        User laura = ServiceTestData.user("laura", "laura@email.com", 17, true);
        stubUser(laura);
        stubEvent(publishedEvent());

        assertThatThrownBy(() -> service.purchase(purchaseBy(laura, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User does not meet minimum age.");

        verify(ticketRepository, never()).save(any());
    }

    // BR-TICKET-006: la edad se evalua en la fecha del evento, no hoy
    @Test
    void purchase_userTurnsAdultBeforeEvent_isAllowed() {
        User user = ServiceTestData.user("sofia", "sofia@email.com", 18, true);
        stubUser(user);
        stubEvent(publishedEvent());
        stubPaidCount(0);
        stubSaveAndMapper();

        TicketResponse result = service.purchase(purchaseBy(user, TicketType.GENERAL));

        assertThat(result.status()).isEqualTo(TicketStatus.PAID);
    }

    @Test
    void purchase_eventWithoutAgeRestriction_ignoresBirthDate() {
        User laura = ServiceTestData.user("laura", "laura@email.com", 17, true);
        Event event = publishedEvent();
        event.setMinimumAge(0);
        stubUser(laura);
        stubEvent(event);
        stubPaidCount(0);
        stubSaveAndMapper();

        assertThat(service.purchase(purchaseBy(laura, TicketType.STUDENT)).status()).isEqualTo(TicketStatus.PAID);
    }

    // TEST-TICKET-007 / AC-009
    @Test
    void purchase_noCapacityLeft_throwsBusinessRule() {
        User user = andrea();
        stubUser(user);
        stubEvent(publishedEvent());
        stubPaidCount(3);

        assertThatThrownBy(() -> service.purchase(purchaseBy(user, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    // TEST-TICKET-008 / AC-008
    @Test
    void purchase_lastTicket_savesTicketAndMarksEventSoldOut() {
        User user = ServiceTestData.user("carlos", "carlos@email.com", 21, true);
        Event event = publishedEvent();
        stubUser(user);
        stubEvent(event);
        stubPaidCount(2);
        stubSaveAndMapper();
        when(eventRepository.save(event)).thenReturn(event);

        service.purchase(purchaseBy(user, TicketType.GENERAL));

        verify(ticketRepository).save(any(Ticket.class));
        verify(eventRepository).save(event);
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
    }

    // TEST-TICKET-009
    @Test
    void cancel_paidTicket_changesStatusToCancelled() {
        Ticket ticket = ServiceTestData.ticket(TicketStatus.PAID, andrea(), publishedEvent());
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(ServiceTestData.toResponse(ticket));

        TicketResponse result = service.cancel("TKT-0001");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(result.status()).isEqualTo(TicketStatus.CANCELLED);
    }

    // TEST-TICKET-010
    @Test
    void cancel_usedTicket_throwsBusinessRule() {
        Ticket ticket = ServiceTestData.ticket(TicketStatus.USED, andrea(), publishedEvent());
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.cancel("TKT-0001")).isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancel_alreadyCancelledTicket_throwsBusinessRule() {
        Ticket ticket = ServiceTestData.ticket(TicketStatus.CANCELLED, andrea(), publishedEvent());
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.cancel("TKT-0001")).isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // BR-TICKET-012
    @Test
    void cancel_afterEventDate_throwsBusinessRule() {
        Event event = publishedEvent();
        event.setEventDate(LocalDateTime.of(2026, 11, 1, 20, 0));
        Ticket ticket = ServiceTestData.ticket(TicketStatus.PAID, andrea(), event);
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.cancel("TKT-0001")).isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-011 / AC-010
    @Test
    void markAsUsed_paidTicket_changesStatusToUsed() {
        Ticket ticket = ServiceTestData.ticket(TicketStatus.PAID, andrea(), publishedEvent());
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(ServiceTestData.toResponse(ticket));

        TicketResponse result = service.markAsUsed("TKT-0001");

        assertThat(result.status()).isEqualTo(TicketStatus.USED);
    }

    // TEST-TICKET-012
    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRule() {
        Ticket ticket = ServiceTestData.ticket(TicketStatus.CANCELLED, andrea(), publishedEvent());
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.markAsUsed("TKT-0001")).isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void findByCode_missingTicket_throwsResourceNotFound() {
        when(ticketRepository.findByTicketCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("NOPE")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findPaidTicketsByEvent_missingEvent_throwsResourceNotFound() {
        when(eventRepository.existsByEventCode("NOPE")).thenReturn(false);

        assertThatThrownBy(() -> service.findPaidTicketsByEvent("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).findByEvent_EventCodeAndStatus(any(), any());
    }

    @Test
    void findPaidTicketsByEvent_existingEvent_queriesPaidStatus() {
        Ticket ticket = ServiceTestData.ticket(TicketStatus.PAID, andrea(), publishedEvent());
        when(eventRepository.existsByEventCode(EVENT)).thenReturn(true);
        when(ticketRepository.findByEvent_EventCodeAndStatus(eq(EVENT), eq(TicketStatus.PAID)))
                .thenReturn(java.util.List.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(ServiceTestData.toResponse(ticket));

        assertThat(service.findPaidTicketsByEvent(EVENT)).hasSize(1);
    }
}
