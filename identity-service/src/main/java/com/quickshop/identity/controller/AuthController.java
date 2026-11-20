package com.quickshop.identity.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.ForgotPasswordRequest;
import com.quickshop.identity.dto.GoogleAuthRequest;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.OtpResponse;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.dto.ResendOtpRequest;
import com.quickshop.identity.dto.ResetPasswordRequest;
import com.quickshop.identity.dto.RevokeTokenRequest;
import com.quickshop.identity.dto.VerifyOtpRequest;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Identity Auth", description = "Endpoints đăng ký, đăng nhập xác thực JWT, OTP qua Redis và Dual Auth (Cookie & Bearer)")
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure = true;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập tài khoản & cấp JWT kèm HttpOnly Cookies")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);

        ResponseCookie accessCookie = createCookie("accessToken", response.getAccessToken(), 86400);
        ResponseCookie refreshCookie = createCookie("refreshToken", response.getRefreshToken(), 604800);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("Đăng nhập thành công", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới & gửi mã OTP xác thực qua email")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);

        if (response.getAccessToken() != null) {
            ResponseCookie accessCookie = createCookie("accessToken", response.getAccessToken(), 86400);
            ResponseCookie refreshCookie = createCookie("refreshToken", response.getRefreshToken(), 604800);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(ApiResponse.created("Đăng ký tài khoản thành công", response));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng ký tài khoản thành công! Vui lòng kiểm tra email và nhập mã OTP 6 chữ số để kích hoạt tài khoản.", response));
    }

    @GetMapping("/check-email")
    @Operation(summary = "Kiểm tra địa chỉ email đã được đăng ký trên hệ thống hay chưa")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEmail(@RequestParam("email") String email) {
        boolean exists = authService.checkEmailExists(email);
        return ResponseEntity.ok(ApiResponse.ok(
                exists ? "Email đã được sử dụng" : "Email khả dụng",
                Map.of("email", email != null ? email.trim() : "", "exists", exists)
        ));
    }

    @PostMapping("/google")
    @Operation(summary = "Đăng nhập, đăng ký hoặc khôi phục tài khoản qua Google OAuth 2.0")
    public ResponseEntity<ApiResponse<AuthResponse>> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.googleAuth(request);

        ResponseCookie accessCookie = createCookie("accessToken", response.getAccessToken(), 86400);
        ResponseCookie refreshCookie = createCookie("refreshToken", response.getRefreshToken(), 604800);

        String message = "FORGOT_PASSWORD".equalsIgnoreCase(request.getAction())
                ? "Khôi phục tài khoản qua Google thành công"
                : ("REGISTER".equalsIgnoreCase(request.getAction())
                ? "Đăng ký tài khoản qua Google thành công"
                : "Đăng nhập qua Google thành công");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok(message, response));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xác thực mã OTP 6 chữ số và kích hoạt tài khoản")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        AuthResponse response = authService.verifyOtp(verifyOtpRequest);

        ResponseCookie accessCookie = createCookie("accessToken", response.getAccessToken(), 86400);
        ResponseCookie refreshCookie = createCookie("refreshToken", response.getRefreshToken(), 604800);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("Xác thực OTP thành công, tài khoản đã được kích hoạt", response));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Yêu cầu gửi lại mã xác thực OTP (giới hạn cooldown 60 giây)")
    public ResponseEntity<ApiResponse<OtpResponse>> resendOtp(@Valid @RequestBody ResendOtpRequest resendOtpRequest) {
        OtpResponse response = authService.resendOtp(resendOtpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Gửi lại mã OTP thành công", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Access Token qua Refresh Token (Body hoặc Cookie)")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken
    ) {
        String token = (request != null && StringUtils.hasText(request.getRefreshToken()))
                ? request.getRefreshToken()
                : cookieRefreshToken;

        if (!StringUtils.hasText(token)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Thiếu Refresh Token"));
        }

        AuthResponse response = authService.refreshToken(token);
        ResponseCookie accessCookie = createCookie("accessToken", response.getAccessToken(), 86400);
        ResponseCookie refreshCookie = createCookie("refreshToken", response.getRefreshToken(), 604800);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("Làm mới token thành công", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản, thu hồi token trong Redis & xóa HttpOnly Cookies")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @CookieValue(name = "accessToken", required = false) String cookieAccess,
            @CookieValue(name = "refreshToken", required = false) String cookieRefresh,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = null;
        if (StringUtils.hasText(userIdHeader)) {
            try {
                userId = Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException ignored) {}
        }

        String accessTokenToRevoke = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            accessTokenToRevoke = authHeader.substring(7).trim();
        } else if (StringUtils.hasText(cookieAccess)) {
            accessTokenToRevoke = cookieAccess.trim();
        }

        // Thu hồi cả Access Token và Refresh Token trong Redis Blacklist
        if (StringUtils.hasText(accessTokenToRevoke)) {
            authService.logout(accessTokenToRevoke, userId);
        }
        if (StringUtils.hasText(cookieRefresh) && !cookieRefresh.trim().equals(accessTokenToRevoke)) {
            authService.revokeToken(cookieRefresh.trim(), userId);
        }
        if (!StringUtils.hasText(accessTokenToRevoke) && !StringUtils.hasText(cookieRefresh)) {
            authService.logout(null, userId);
        }

        ResponseCookie cleanAccess = createCookie("accessToken", "", 0);
        ResponseCookie cleanRefresh = createCookie("refreshToken", "", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanAccess.toString())
                .header(HttpHeaders.SET_COOKIE, cleanRefresh.toString())
                .body(ApiResponse.ok("Đăng xuất thành công"));
    }

    @PostMapping("/revoke")
    @Operation(summary = "Thu hồi Access/Refresh Token (Blacklist Redis)")
    public ResponseEntity<ApiResponse<String>> revokeToken(
            @Valid @RequestBody RevokeTokenRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = StringUtils.hasText(userIdHeader) ? Long.parseLong(userIdHeader.trim()) : null;
        authService.revokeToken(request.getToken(), userId);
        return ResponseEntity.ok(ApiResponse.ok("Token đã được thu hồi thành công"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu cấp mã OTP đặt lại mật khẩu qua Email (TTL 15 phút, cooldown 60s)")
    public ResponseEntity<ApiResponse<OtpResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        OtpResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Mã xác thực đặt lại mật khẩu đã được gửi tới email của bạn", response));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Xác thực mã OTP và đặt lại mật khẩu mới")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới"));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa xác thực danh tính"));
        }
        User user = authService.getCurrentUser(Long.parseLong(userIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    private ResponseCookie createCookie(String name, String value, long maxAgeSecs) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSecs)
                .build();
    }

    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
