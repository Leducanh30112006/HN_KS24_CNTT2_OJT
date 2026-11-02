package com.quickshop.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.entity.Role;
import com.quickshop.identity.entity.User;
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
@DisplayName("AuthController - REST APIs Đăng ký, Đăng nhập, Dual Auth Cookie & Refresh Token")
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
}
