package com.pulse.PulsePass.service.impl;

import com.pulse.PulsePass.domain.Event;
import com.pulse.PulsePass.domain.Ticket;
import com.pulse.PulsePass.domain.User;
import com.pulse.PulsePass.domain.UserProfile;
import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.domain.enums.TicketStatus;
import com.pulse.PulsePass.dto.request.PurchaseTicketRequest;
import com.pulse.PulsePass.dto.response.TicketResponse;
import com.pulse.PulsePass.exception.BusinessRuleException;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.mapper.TicketMapper;
import com.pulse.PulsePass.repository.EventRepository;
import com.pulse.PulsePass.repository.TicketRepository;
import com.pulse.PulsePass.repository.UserRepository;
import com.pulse.PulsePass.service.TicketService;
import com.pulse.PulsePass.service.pricing.TicketPriceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;
    private final TicketPriceCalculator priceCalculator;
    private final Clock clock;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            TicketMapper ticketMapper,
            TicketPriceCalculator priceCalculator,
            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.priceCalculator = priceCalculator;
        this.clock = clock;
    }

    /** Operacion atomica: si cualquier validacion falla se hace rollback completo. */
    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        if (request == null
                || request.userEmail() == null
                || request.eventCode() == null
                || request.type() == null) {
            throw new BusinessRuleException("userEmail, eventCode and type are required.");
        }

        // BR-TICKET-001 / 002
        User user =
                userRepository
                        .findByEmailIgnoreCase(request.userEmail())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found: " + request.userEmail()));
        if (!user.isActive()) {
            throw new BusinessRuleException("User is not active: " + request.userEmail());
        }

        // BR-TICKET-003 / 004 / 005
        Event event =
                eventRepository
                        .findByEventCode(request.eventCode())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Event not found: " + request.eventCode()));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events. Current status: "
                            + event.getStatus());
        }
        if (!event.getEventDate().isAfter(now())) {
            throw new BusinessRuleException(
                    "Event has already taken place: " + event.getEventCode());
        }

        // BR-TICKET-006
        validateMinimumAge(user, event);

        // BR-TICKET-007
        long paidTickets =
                ticketRepository.countByEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID);
        int capacity = event.getVenue().getCapacity();
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no capacity left: " + event.getEventCode());
        }

        // BR-TICKET-009
        BigDecimal price = priceCalculator.calculate(request.type());

        Ticket ticket =
                new Ticket(
                        generateTicketCode(),
                        request.type(),
                        price,
                        TicketStatus.PAID,
                        now(),
                        user,
                        event);
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: misma transaccion que la creacion del ticket
        if (paidTickets + 1 >= capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(getTicket(ticketCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(email).stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        if (!eventRepository.existsByEventCode(eventCode)) {
            throw new ResourceNotFoundException("Event not found: " + eventCode);
        }
        return ticketRepository
                .findByEvent_EventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = getTicket(ticketCode);

        // BR-TICKET-010 / 011
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current status: " + ticket.getStatus());
        }
        // BR-TICKET-012
        if (now().isAfter(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException(
                    "Ticket cannot be cancelled after the event date: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = getTicket(ticketCode);

        // BR-TICKET-014
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("A CANCELLED ticket cannot be used: " + ticketCode);
        }
        // BR-TICKET-013
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be marked as used. Current status: "
                            + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    private Ticket getTicket(String ticketCode) {
        return ticketRepository
                .findByTicketCode(ticketCode)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    /** La edad se evalua en la fecha del evento, no en la fecha de compra. */
    private void validateMinimumAge(User user, Event event) {
        if (event.getMinimumAge() <= 0) {
            return;
        }
        UserProfile profile = user.getProfile();
        if (profile == null || profile.getBirthDate() == null) {
            throw new BusinessRuleException(
                    "User birth date is required to purchase tickets for age-restricted events.");
        }
        LocalDate eventDay = event.getEventDate().toLocalDate();
        int ageAtEvent = Period.between(profile.getBirthDate(), eventDay).getYears();
        if (ageAtEvent < event.getMinimumAge()) {
            throw new BusinessRuleException("User does not meet minimum age.");
        }
    }

    private String generateTicketCode() {
        return "TKT-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
