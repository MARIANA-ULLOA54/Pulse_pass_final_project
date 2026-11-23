package com.pulse.PulsePass.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 60, message = "Username must have at most 60 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must have at most 150 characters")
        String email,

        @NotBlank(message = "First name is required")
        @Size(max = 80, message = "First name must have at most 80 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80, message = "Last name must have at most 80 characters")
        String lastName,

        @Size(max = 30, message = "Phone must have at most 30 characters")
        String phone,

        @Size(max = 100, message = "City must have at most 100 characters")
        String city,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate,

        @NotBlank @Size(min = 12) String password
) {
    /** Constructor para pruebas heredadas; el registro HTTP siempre exige password. */
    public RegisterUserRequest(String username,String email,String firstName,String lastName,String phone,String city,LocalDate birthDate) {
        this(username,email,firstName,lastName,phone,city,birthDate,null);
    }
}
