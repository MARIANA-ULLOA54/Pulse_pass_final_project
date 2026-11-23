package com.pulse.PulsePass.security;

import tools.jackson.databind.ObjectMapper;
import com.pulse.PulsePass.dto.response.ErrorResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final CustomUserDetailsService users;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtService jwt, CustomUserDetailsService users, ObjectMapper mapper) {
        this.jwt = jwt;
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var headers = request.getHeaders("Authorization");
        if (!headers.hasMoreElements()) {
            chain.doFilter(request, response);
            return;
        }
        String header = headers.nextElement();
        if (headers.hasMoreElements()
                || header == null
                || !header.startsWith("Bearer ")
                || header.length() <= 7
                || header.substring(7).isBlank()) {
            SecurityContextHolder.clearContext();
            writeError(response, HttpStatus.UNAUTHORIZED, "Invalid Bearer token");
            return;
        }
        try {
            Long id = jwt.verify(header.substring(7));
            var principal = users.loadById(id);
            if (!principal.isEnabled()) {
                throw new UsernameNotFoundException("Inactive account");
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException | AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            writeError(response, HttpStatus.UNAUTHORIZED, "Invalid Bearer token");
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ErrorResponse(
            LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, Map.of()));
    }
}
