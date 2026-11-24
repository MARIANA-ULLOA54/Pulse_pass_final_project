package com.pulse.PulsePass.service;

import com.pulse.PulsePass.dto.request.RegisterUserRequest;
import com.pulse.PulsePass.dto.response.UserResponse;

public interface UserService {

    UserResponse register(RegisterUserRequest request);

    UserResponse findByEmail(String email);

    UserResponse findByUsername(String username);
}
