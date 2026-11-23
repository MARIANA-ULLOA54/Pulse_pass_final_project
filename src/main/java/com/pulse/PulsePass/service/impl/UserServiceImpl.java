package com.pulse.PulsePass.service.impl;

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
import com.pulse.PulsePass.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    private final Clock clock;

    public UserServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserMapper userMapper,
            Clock clock) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userMapper = userMapper;
        this.clock = clock;
    }

    /** BR-USER-004: User y UserProfile se crean en la misma transaccion. */
    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        validateRequiredFields(request);

        // BR-USER-001
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists.");
        }
        // BR-USER-002
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already exists.");
        }
        // BR-USER-005
        if (request.birthDate() != null && request.birthDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("Birth date cannot be in the future.");
        }

        // BR-USER-003
        User user = new User(request.username(), request.email(), true);
        UserProfile profile =
                new UserProfile(
                        request.firstName(),
                        request.lastName(),
                        request.phone(),
                        request.city(),
                        request.birthDate());
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        profile.setUser(savedUser);
        userProfileRepository.save(profile);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void validateRequiredFields(RegisterUserRequest request) {
        if (request == null
                || isBlank(request.username())
                || isBlank(request.email())
                || isBlank(request.firstName())
                || isBlank(request.lastName())) {
            throw new BusinessRuleException(
                    "username, email, firstName and lastName are required.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
