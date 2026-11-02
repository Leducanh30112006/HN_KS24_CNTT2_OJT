package com.quickshop.identity.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Identity Auth", description = "Endpoints đăng ký, đăng nhập xác thực JWT và Dual Auth (Cookie & Bearer)")
public class AuthController {

    private final AuthService authService;

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
    @Operation(summary = "Đăng ký tài khoản mới & tự động cấp JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);

        ResponseCookie accessCookie = createCookie("accessToken", response.getAccessToken(), 86400);
        ResponseCookie refreshCookie = createCookie("refreshToken", response.getRefreshToken(), 604800);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.created("Đăng ký tài khoản thành công", response));
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
    @Operation(summary = "Đăng xuất tài khoản & xóa HttpOnly Cookies")
    public ResponseEntity<ApiResponse<String>> logout() {
        ResponseCookie cleanAccess = createCookie("accessToken", "", 0);
        ResponseCookie cleanRefresh = createCookie("refreshToken", "", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanAccess.toString())
                .header(HttpHeaders.SET_COOKIE, cleanRefresh.toString())
                .body(ApiResponse.ok("Đăng xuất thành công"));
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
                .secure(false) // Set true in production behind HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSecs)
                .build();
    }

    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
