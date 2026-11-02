package com.quickshop.identity.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.entity.Role;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.entity.UserStatus;
import com.quickshop.identity.repository.ShopRepository;
import com.quickshop.identity.repository.UserRepository;
import com.quickshop.identity.security.JwtTokenProvider;
import com.quickshop.identity.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Quản lý Đăng ký, Đăng nhập, Mã hóa mật khẩu và Token")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("testbuyer")
                .email("buyer@quickshop.vn")
                .phoneNumber("0987654321")
                .password("encoded_pass")
                .fullName("Nguyễn Văn Mua")
                .role(Role.ROLE_BUYER)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("testbuyer")
                .email("buyer@quickshop.vn")
                .phoneNumber("0987654321")
                .password("raw_password")
                .fullName("Nguyễn Văn Mua")
                .role(Role.ROLE_BUYER)
                .build();

        loginRequest = LoginRequest.builder()
                .usernameOrEmail("testbuyer")
                .password("raw_password")
                .build();
    }

    @Nested
    @DisplayName("Nghiệp vụ Đăng ký tài khoản (Register)")
    class RegisterTests {

        @Test
        @DisplayName("Đăng ký thành công - hợp lệ username, email, phone")
        void register_Success() {
            when(userRepository.existsByUsername("testbuyer")).thenReturn(false);
            when(userRepository.existsByEmail("buyer@quickshop.vn")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("0987654321")).thenReturn(false);
            when(passwordEncoder.encode("raw_password")).thenReturn("encoded_pass");
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);
            when(tokenProvider.generateAccessToken(sampleUser)).thenReturn("mock_access_token");
            when(tokenProvider.generateRefreshToken(sampleUser)).thenReturn("mock_refresh_token");

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock_access_token");
            assertThat(response.getRefreshToken()).isEqualTo("mock_refresh_token");
            assertThat(response.getUsername()).isEqualTo("testbuyer");
            assertThat(response.getRole()).isEqualTo(Role.ROLE_BUYER);
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại - trùng username")
        void register_DuplicateUsername_ThrowsBadRequest() {
            when(userRepository.existsByUsername("testbuyer")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Username đã tồn tại");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại - trùng email")
        void register_DuplicateEmail_ThrowsBadRequest() {
            when(userRepository.existsByUsername("testbuyer")).thenReturn(false);
            when(userRepository.existsByEmail("buyer@quickshop.vn")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email đã được đăng ký");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại - trùng số điện thoại")
        void register_DuplicatePhoneNumber_ThrowsBadRequest() {
            when(userRepository.existsByUsername("testbuyer")).thenReturn(false);
            when(userRepository.existsByEmail("buyer@quickshop.vn")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("0987654321")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số điện thoại đã được đăng ký");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đăng nhập tài khoản (Login)")
    class LoginTests {

        @Test
        @DisplayName("Đăng nhập thành công - đúng mật khẩu và tài khoản đang hoạt động")
        void login_Success() {
            when(userRepository.findByUsernameOrEmail("testbuyer", "testbuyer")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("raw_password", "encoded_pass")).thenReturn(true);
            when(tokenProvider.generateAccessToken(sampleUser)).thenReturn("mock_access_token");
            when(tokenProvider.generateRefreshToken(sampleUser)).thenReturn("mock_refresh_token");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock_access_token");
            assertThat(response.getRole()).isEqualTo(Role.ROLE_BUYER);
        }

        @Test
        @DisplayName("Đăng nhập thất bại - sai mật khẩu")
        void login_WrongPassword_ThrowsBadRequest() {
            when(userRepository.findByUsernameOrEmail("testbuyer", "testbuyer")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("raw_password", "encoded_pass")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("không chính xác");
        }

        @Test
        @DisplayName("Đăng nhập thất bại - tài khoản bị khóa")
        void login_BlockedUser_ThrowsBadRequest() {
            sampleUser.setStatus(UserStatus.BLOCKED);
            when(userRepository.findByUsernameOrEmail("testbuyer", "testbuyer")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("raw_password", "encoded_pass")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("đã bị khóa");
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Làm mới token thành công")
        void refreshToken_Success() {
            when(tokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
            when(tokenProvider.getUsernameFromToken("valid_refresh_token")).thenReturn("testbuyer");
            when(userRepository.findByUsername("testbuyer")).thenReturn(Optional.of(sampleUser));
            when(tokenProvider.generateAccessToken(sampleUser)).thenReturn("new_access_token");
            when(tokenProvider.generateRefreshToken(sampleUser)).thenReturn("new_refresh_token");

            AuthResponse response = authService.refreshToken("valid_refresh_token");

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new_access_token");
            assertThat(response.getRefreshToken()).isEqualTo("new_refresh_token");
        }

        @Test
        @DisplayName("Làm mới token thất bại - token không hợp lệ")
        void refreshToken_Invalid_ThrowsBadRequest() {
            when(tokenProvider.validateToken("invalid_token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("invalid_token"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Refresh token không hợp lệ");
        }
    }
}

