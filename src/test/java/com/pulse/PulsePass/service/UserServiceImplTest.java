package com.pulse.PulsePass.service;

import com.pulse.PulsePass.domain.User;
import com.pulse.PulsePass.domain.UserProfile;
import com.pulse.PulsePass.dto.request.RegisterUserRequest;
import com.pulse.PulsePass.dto.response.UserResponse;
import com.pulse.PulsePass.exception.BusinessRuleException;
import com.pulse.PulsePass.exception.DuplicateResourceException;
import com.pulse.PulsePass.exception.ResourceNotFoundException;
import com.pulse.PulsePass.mapper.UserMapper;
import com.pulse.PulsePass.repository.UserProfileRepository;
import com.pulse.PulsePass.repository.UserRepository;
import com.pulse.PulsePass.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserMapper userMapper;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, userProfileRepository, userMapper,
                ServiceTestData.CLOCK);
    }

    private RegisterUserRequest request(LocalDate birthDate) {
        return new RegisterUserRequest("andrea", "andrea@email.com", "Andrea", "Lopez",
                "3001234567", "Santa Marta", birthDate);
    }

    // TEST-USER-001 / BR-USER-003 / BR-USER-004
    @Test
    void register_validUser_savesActiveUserAndProfile() {
        UserResponse expected = new UserResponse(1L, "andrea", "andrea@email.com", true,
                "Andrea", "Lopez", "3001234567", "Santa Marta", LocalDate.of(2001, 5, 10));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        UserResponse result = service.register(request(LocalDate.of(2001, 5, 10)));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userRepository).save(userCaptor.capture());
        verify(userProfileRepository).save(profileCaptor.capture());
        assertThat(userCaptor.getValue().isActive()).isTrue();
        assertThat(profileCaptor.getValue().getUser()).isSameAs(userCaptor.getValue());
        assertThat(profileCaptor.getValue().getFirstName()).isEqualTo("Andrea");
        assertThat(result).isEqualTo(expected);
    }

    // TEST-USER-002
    @Test
    void register_duplicatedUsername_throwsDuplicate() {
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request(LocalDate.of(2001, 5, 10))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists.");

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    // TEST-USER-003
    @Test
    void register_duplicatedEmail_throwsDuplicate() {
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request(LocalDate.of(2001, 5, 10))))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    // TEST-USER-004
    @Test
    void register_futureBirthDate_throwsBusinessRule() {
        assertThatThrownBy(() -> service.register(request(LocalDate.of(2027, 1, 1))))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void findByEmail_missingUser_throwsResourceNotFound() {
        when(userRepository.findByEmailIgnoreCase("x@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByEmail("x@email.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByUsername_existingUser_returnsDto() {
        User user = new User("andrea", "andrea@email.com", true);
        UserResponse expected = new UserResponse(1L, "andrea", "andrea@email.com", true,
                null, null, null, null, null);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        assertThat(service.findByUsername("andrea")).isEqualTo(expected);
    }
}
