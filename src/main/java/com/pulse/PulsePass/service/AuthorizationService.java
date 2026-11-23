package com.pulse.PulsePass.service;

import com.pulse.PulsePass.domain.enums.EventStatus;
import com.pulse.PulsePass.repository.*;
import com.pulse.PulsePass.security.SecurityPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
@Service("authorizationService")
public class AuthorizationService {
 private final UserRepository users;private final TicketRepository tickets;private final EventRepository events;
 public AuthorizationService(UserRepository users,TicketRepository tickets,EventRepository events){this.users=users;this.tickets=tickets;this.events=events;}
 private SecurityPrincipal principal(){Object p=SecurityContextHolder.getContext().getAuthentication()==null?null:SecurityContextHolder.getContext().getAuthentication().getPrincipal();if(p instanceof SecurityPrincipal s)return s;throw new AccessDeniedException("Authentication required");}
 public boolean privileged(){return principal().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")||a.getAuthority().equals("ROLE_STAFF"));}
 public boolean admin(){return principal().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));}
 public boolean ownEmail(String email){return users.findByEmailIgnoreCase(email).map(u->u.getId().equals(principal().userId())).orElse(false);}
 public boolean ownUsername(String username){return users.findByUsername(username).map(u->u.getId().equals(principal().userId())).orElse(false);}
 public boolean ownTicket(String code){return tickets.findByTicketCode(code).map(t->t.getUser().getId().equals(principal().userId())).orElse(false);}
 public boolean ticketReadable(String code){return privileged()||ownTicket(code);}
 public boolean ticketCancellable(String code){return admin()||ownTicket(code);}
 public boolean publicEvent(String code){return events.findByEventCode(code).map(e->e.getStatus()==EventStatus.PUBLISHED).orElse(false);}
 public boolean mayReadEvent(String code){return publicEvent(code)||privileged();}
 public boolean canReadOtherEvents(){return SecurityContextHolder.getContext().getAuthentication()!=null && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof SecurityPrincipal && privileged();}
}
