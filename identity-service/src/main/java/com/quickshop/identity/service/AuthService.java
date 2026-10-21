package com.quickshop.identity.service;

import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.RegisterRequest;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse register(RegisterRequest registerRequest);
}
