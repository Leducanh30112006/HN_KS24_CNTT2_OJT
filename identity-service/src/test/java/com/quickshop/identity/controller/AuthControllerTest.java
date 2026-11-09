package com.quickshop.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.ForgotPasswordRequest;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.OtpResponse;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.dto.ResendOtpRequest;
import com.quickshop.identity.dto.ResetPasswordRequest;
import com.quickshop.identity.dto.RevokeTokenRequest;
import com.quickshop.identity.dto.VerifyOtpRequest;
import com.quickshop.identity.entity.Role;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.entity.UserStatus;
import com.quickshop.identity.exception.GlobalExceptionHandler;
import com.quickshop.identity.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController - REST APIs Đăng ký, Đăng nhập, OTP Redis & Dual Auth Cookie")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register - Đăng ký thành công trả về 201 Created và HttpOnly Cookies")
    void register_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@quickshop.vn")
                .phoneNumber("0988776655")
                .password("password123")
                .fullName("Test User")
                .role(Role.ROLE_BUYER)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock_access_token")
                .refreshToken("mock_refresh_token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(1L)
                .username("testuser")
                .email("test@quickshop.vn")
                .fullName("Test User")
                .role(Role.ROLE_BUYER)
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation thất bại khi thiếu email hoặc password ngắn")
    void register_ValidationFailed() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("ab") // quá ngắn (< 3)
                .email("invalid-email") // sai định dạng
                .password("123") // quá ngắn (< 6)
                .fullName("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/verify-otp - Xác thực OTP thành công trả về 200 OK và cấp JWT")
    void verifyOtp_Success() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("buyer@quickshop.vn")
                .otp("123456")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("verified_access_token")
                .refreshToken("verified_refresh_token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(1L)
                .username("testbuyer")
                .email("buyer@quickshop.vn")
                .fullName("Nguyễn Văn Mua")
                .role(Role.ROLE_BUYER)
                .status(UserStatus.ACTIVE)
                .build();

        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("verified_access_token"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/auth/verify-otp - Validation thất bại khi OTP không đúng 6 số")
    void verifyOtp_ValidationFailed_InvalidOtp() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("buyer@quickshop.vn")
                .otp("123") // chỉ 3 số
                .build();

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/resend-otp - Yêu cầu gửi lại OTP thành công trả về 200 OK")
    void resendOtp_Success() throws Exception {
        ResendOtpRequest request = ResendOtpRequest.builder()
                .email("buyer@quickshop.vn")
                .build();

        OtpResponse otpResponse = OtpResponse.builder()
                .email("buyer@quickshop.vn")
                .message("Mã OTP mới đã được gửi thành công")
                .ttlSeconds(300L)
                .cooldownSeconds(60L)
                .build();

        when(authService.resendOtp(any(ResendOtpRequest.class))).thenReturn(otpResponse);

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("buyer@quickshop.vn"))
                .andExpect(jsonPath("$.data.ttlSeconds").value(300))
                .andExpect(jsonPath("$.data.cooldownSeconds").value(60));
    }

    @Test
    @DisplayName("POST /api/auth/resend-otp - Validation thất bại khi email không đúng định dạng")
    void resendOtp_ValidationFailed_InvalidEmail() throws Exception {
        ResendOtpRequest request = ResendOtpRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login - Đăng nhập thành công trả về 200 OK và Token")
    void login_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock_access_token")
                .refreshToken("mock_refresh_token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(1L)
                .username("testuser")
                .email("test@quickshop.vn")
                .fullName("Test User")
                .role(Role.ROLE_BUYER)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Sai mật khẩu trả về 400 Bad Request")
    void login_Failed_WrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không chính xác"));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Đăng xuất thành công và xóa cookies")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Đăng xuất thành công"));
    }

    @Test
    @DisplayName("GET /api/auth/me - Trả về thông tin tài khoản khi có X-User-Id header")
    void getCurrentUser_Success() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@quickshop.vn")
                .fullName("Test User")
                .role(Role.ROLE_BUYER)
                .build();

        when(authService.getCurrentUser(1L)).thenReturn(user);

        mockMvc.perform(get("/api/auth/me")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Yêu cầu cấp OTP đặt lại mật khẩu")
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("buyer@quickshop.vn")
                .build();

        OtpResponse response = OtpResponse.builder()
                .email("buyer@quickshop.vn")
                .message("OTP sent")
                .ttlSeconds(900L)
                .cooldownSeconds(60L)
                .build();

        when(authService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ttlSeconds").value(900));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Đặt lại mật khẩu mới với mã OTP")
    void resetPassword_Success() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("buyer@quickshop.vn")
                .otp("123456")
                .newPassword("NewPass123")
                .confirmPassword("NewPass123")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/revoke - Thu hồi token thành công")
    void revokeToken_Success() throws Exception {
        RevokeTokenRequest request = RevokeTokenRequest.builder()
                .token("token_to_blacklist")
                .build();

        mockMvc.perform(post("/api/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
