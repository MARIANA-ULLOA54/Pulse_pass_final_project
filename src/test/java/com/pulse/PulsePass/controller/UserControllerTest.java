package com.pulse.PulsePass.controller;

import com.pulse.PulsePass.dto.request.RegisterUserRequest;
import com.pulse.PulsePass.exception.DuplicateResourceException;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // TEST-CTRL-USR-001 / AC-CTRL007
    @Test
    void register_validRequest_returns201() throws Exception {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(ControllerTestData.user());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestData.VALID_USER_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.active").value(true));

        ArgumentCaptor<RegisterUserRequest> captor = ArgumentCaptor.forClass(RegisterUserRequest.class);
        verify(userService).register(captor.capture());
        assertThat(captor.getValue().birthDate()).isEqualTo(LocalDate.of(2001, 5, 10));
    }

    // TEST-CTRL-USR-002
    @Test
    void register_invalidEmail_returns400AndDoesNotCallService() throws Exception {
        String invalidEmail = ControllerTestData.VALID_USER_JSON.replace("andrea@email.com", "not-an-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmail))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email must be valid"));

        verify(userService, never()).register(any());
    }

    @Test
    void register_missingRequiredFields_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.firstName").exists())
                .andExpect(jsonPath("$.details.lastName").exists())
                .andExpect(jsonPath("$.details.birthDate").exists());

        verify(userService, never()).register(any());
    }

    // TEST-CTRL-USR-003 / AC-CTRL008
    @Test
    void register_duplicatedUsername_returns409() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists."));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestData.VALID_USER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username already exists."));
    }

    // TEST-CTRL-USR-004
    @Test
    void findByEmail_returns200() throws Exception {
        when(userService.findByEmail("andrea@email.com")).thenReturn(ControllerTestData.user());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("andrea@email.com"));

        verify(userService).findByEmail("andrea@email.com");
    }

    @Test
    void findByEmail_missingUser_returns404() throws Exception {
        when(userService.findByEmail("x@email.com")).thenThrow(new ResourceNotFoundException("User not found: x@email.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "x@email.com"))
                .andExpect(status().isNotFound());
    }

    // TEST-CTRL-USR-005
    @Test
    void findByUsername_returns200() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(ControllerTestData.user());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }
}
