package com.pulse.PulsePass.security;

import com.pulse.PulsePass.dto.response.ErrorResponse;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> invalid(AuthenticationException e) {
        return ResponseEntity.status(401)
                .body(
                        new ErrorResponse(
                                LocalDateTime.now(),
                                401,
                                "Unauthorized",
                                "Credenciales invalidas",
                                Map.of()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(Exception e) {
        return ResponseEntity.status(403)
                .body(
                        new ErrorResponse(
                                LocalDateTime.now(),
                                403,
                                "Forbidden",
                                "No tiene permisos para realizar esta operacion",
                                Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> invalidInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(
                        new ErrorResponse(
                                LocalDateTime.now(), 400, "Bad Request", e.getMessage(), Map.of()));
    }
}
