package com.quickshop.identity.service;

import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.OtpResponse;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.dto.ResendOtpRequest;
import com.quickshop.identity.dto.VerifyOtpRequest;
import com.quickshop.identity.entity.User;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse register(RegisterRequest registerRequest);
    AuthResponse refreshToken(String refreshToken);
    User getCurrentUser(Long userId);
    AuthResponse verifyOtp(VerifyOtpRequest verifyOtpRequest);
    OtpResponse resendOtp(ResendOtpRequest resendOtpRequest);
}
