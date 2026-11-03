package com.quickshop.identity.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.OtpResponse;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.dto.ResendOtpRequest;
import com.quickshop.identity.dto.VerifyOtpRequest;
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
import org.mockito.ArgumentCaptor;
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
@DisplayName("AuthService - Quản lý Đăng ký, Đăng nhập, OTP Redis TTL 5 phút và Token")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private OtpService otpService;

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
        @DisplayName("Đăng ký thành công - hợp lệ username, email, phone và kích hoạt gửi OTP qua Redis")
        void register_Success() {
            when(userRepository.existsByUsername("testbuyer")).thenReturn(false);
            when(userRepository.existsByEmail("buyer@quickshop.vn")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("0987654321")).thenReturn(false);
            when(passwordEncoder.encode("raw_password")).thenReturn("encoded_pass");
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);
            when(otpService.generateAndSaveOtp("buyer@quickshop.vn")).thenReturn("123456");
            when(tokenProvider.generateAccessToken(sampleUser)).thenReturn("mock_access_token");
            when(tokenProvider.generateRefreshToken(sampleUser)).thenReturn("mock_refresh_token");

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock_access_token");
            assertThat(response.getRefreshToken()).isEqualTo("mock_refresh_token");
            assertThat(response.getUsername()).isEqualTo("testbuyer");
            assertThat(response.getRole()).isEqualTo(Role.ROLE_BUYER);
            verify(userRepository, times(1)).save(any(User.class));
            verify(otpService, times(1)).generateAndSaveOtp("buyer@quickshop.vn");
        }

        @Test
        @DisplayName("Đăng ký lưu user ở trạng thái PENDING_VERIFICATION và active=false")
        void register_CreatesPendingVerificationUser() {
            when(userRepository.existsByUsername("testbuyer")).thenReturn(false);
            when(userRepository.existsByEmail("buyer@quickshop.vn")).thenReturn(false);
            when(passwordEncoder.encode("raw_password")).thenReturn("encoded_pass");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            authService.register(registerRequest);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
            assertThat(saved.isActive()).isFalse();
            verify(otpService).generateAndSaveOtp("buyer@quickshop.vn");
        }

        @Test
        @DisplayName("Đăng ký thất bại - trùng username")
        void register_DuplicateUsername_ThrowsBadRequest() {
            when(userRepository.existsByUsername("testbuyer")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Username đã tồn tại");

            verify(userRepository, never()).save(any(User.class));
            verify(otpService, never()).generateAndSaveOtp(anyString());
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
            verify(otpService, never()).generateAndSaveOtp(anyString());
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
            verify(otpService, never()).generateAndSaveOtp(anyString());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Xác thực OTP (verifyOtp)")
    class VerifyOtpTests {

        @Test
        @DisplayName("Xác thực OTP thành công - kích hoạt tài khoản ACTIVE và cấp JWT")
        void verifyOtp_Success_ActivatesUser() {
            User pendingUser = User.builder()
                    .id(2L)
                    .username("newuser")
                    .email("new@quickshop.vn")
                    .fullName("New User")
                    .role(Role.ROLE_BUYER)
                    .status(UserStatus.PENDING_VERIFICATION)
                    .active(false)
                    .build();

            when(userRepository.findByEmail("new@quickshop.vn")).thenReturn(Optional.of(pendingUser));
            doNothing().when(otpService).verifyOtp("new@quickshop.vn", "123456");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(tokenProvider.generateAccessToken(any(User.class))).thenReturn("access_token_123");
            when(tokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh_token_123");

            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("new@quickshop.vn")
                    .otp("123456")
                    .build();

            AuthResponse response = authService.verifyOtp(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access_token_123");
            assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(pendingUser.isActive()).isTrue();
            verify(otpService, times(1)).verifyOtp("new@quickshop.vn", "123456");
            verify(userRepository, times(1)).save(pendingUser);
        }

        @Test
        @DisplayName("Xác thực OTP thành công - tài khoản đã ACTIVE từ trước trả về token ngay")
        void verifyOtp_AlreadyActive_ReturnsTokens() {
            when(userRepository.findByEmail("buyer@quickshop.vn")).thenReturn(Optional.of(sampleUser));
            when(tokenProvider.generateAccessToken(sampleUser)).thenReturn("access_token_123");
            when(tokenProvider.generateRefreshToken(sampleUser)).thenReturn("refresh_token_123");

            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("buyer@quickshop.vn")
                    .otp("123456")
                    .build();

            AuthResponse response = authService.verifyOtp(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access_token_123");
            verify(otpService, never()).verifyOtp(anyString(), anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Xác thực OTP thất bại - email không tồn tại trên hệ thống")
        void verifyOtp_UserNotFound_ThrowsBadRequest() {
            when(userRepository.findByEmail("notfound@quickshop.vn")).thenReturn(Optional.empty());

            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("notfound@quickshop.vn")
                    .otp("123456")
                    .build();

            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không tìm thấy tài khoản");

            verify(otpService, never()).verifyOtp(anyString(), anyString());
        }

        @Test
        @DisplayName("Xác thực OTP thất bại - mã OTP sai hoặc hết hạn do OtpService ném ngoại lệ")
        void verifyOtp_WrongOtp_PropagatesException() {
            User pendingUser = User.builder()
                    .id(2L)
                    .email("new@quickshop.vn")
                    .status(UserStatus.PENDING_VERIFICATION)
                    .active(false)
                    .build();

            when(userRepository.findByEmail("new@quickshop.vn")).thenReturn(Optional.of(pendingUser));
            doThrow(new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại"))
                    .when(otpService).verifyOtp("new@quickshop.vn", "000000");

            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("new@quickshop.vn")
                    .otp("000000")
                    .build();

            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Mã OTP không chính xác");

            assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Gửi lại OTP (resendOtp)")
    class ResendOtpTests {

        @Test
        @DisplayName("Gửi lại OTP thành công cho tài khoản PENDING_VERIFICATION")
        void resendOtp_Success() {
            User pendingUser = User.builder()
                    .id(2L)
                    .email("new@quickshop.vn")
                    .status(UserStatus.PENDING_VERIFICATION)
                    .active(false)
                    .build();

            when(userRepository.findByEmail("new@quickshop.vn")).thenReturn(Optional.of(pendingUser));
            when(otpService.resendOtp("new@quickshop.vn")).thenReturn("654321");

            ResendOtpRequest request = ResendOtpRequest.builder()
                    .email("new@quickshop.vn")
                    .build();

            OtpResponse response = authService.resendOtp(request);

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("new@quickshop.vn");
            assertThat(response.getTtlSeconds()).isEqualTo(300L);
            assertThat(response.getCooldownSeconds()).isEqualTo(60L);
            verify(otpService, times(1)).resendOtp("new@quickshop.vn");
        }

        @Test
        @DisplayName("Gửi lại OTP thất bại khi tài khoản đã ACTIVE")
        void resendOtp_AlreadyActive_ThrowsBadRequest() {
            when(userRepository.findByEmail("buyer@quickshop.vn")).thenReturn(Optional.of(sampleUser));

            ResendOtpRequest request = ResendOtpRequest.builder()
                    .email("buyer@quickshop.vn")
                    .build();

            assertThatThrownBy(() -> authService.resendOtp(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Tài khoản này đã được kích hoạt thành công");

            verify(otpService, never()).resendOtp(anyString());
        }

        @Test
        @DisplayName("Gửi lại OTP thất bại khi email không tồn tại")
        void resendOtp_UserNotFound_ThrowsBadRequest() {
            when(userRepository.findByEmail("notfound@quickshop.vn")).thenReturn(Optional.empty());

            ResendOtpRequest request = ResendOtpRequest.builder()
                    .email("notfound@quickshop.vn")
                    .build();

            assertThatThrownBy(() -> authService.resendOtp(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không tìm thấy tài khoản");

            verify(otpService, never()).resendOtp(anyString());
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
        @DisplayName("Đăng nhập thất bại - tài khoản chưa kích hoạt OTP (PENDING_VERIFICATION)")
        void login_PendingVerificationUser_ThrowsBadRequest() {
            sampleUser.setStatus(UserStatus.PENDING_VERIFICATION);
            when(userRepository.findByUsernameOrEmail("testbuyer", "testbuyer")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("raw_password", "encoded_pass")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Tài khoản chưa được kích hoạt");
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
