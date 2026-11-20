package com.quickshop.identity.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.ForgotPasswordRequest;
import com.quickshop.identity.dto.GoogleAuthRequest;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.OtpResponse;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.dto.ResendOtpRequest;
import com.quickshop.identity.dto.ResetPasswordRequest;
import com.quickshop.identity.dto.VerifyOtpRequest;
import com.quickshop.identity.entity.Role;
import com.quickshop.identity.entity.Shop;
import com.quickshop.identity.entity.ShopStatus;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.entity.UserStatus;
import com.quickshop.identity.repository.ShopRepository;
import com.quickshop.identity.repository.UserRepository;
import com.quickshop.identity.security.JwtTokenProvider;
import com.quickshop.identity.service.AuditLogService;
import com.quickshop.identity.service.AuthService;
import com.quickshop.identity.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    public static final String REVOKED_TOKEN_PREFIX = "identity:revoked_token:";

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate;
    private final com.quickshop.identity.service.EmailService emailService;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${google.client-secret:}")
    private String googleClientSecret;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        String identifier = loginRequest != null && loginRequest.getUsernameOrEmail() != null
                ? loginRequest.getUsernameOrEmail().trim()
                : "";
        String rawPassword = loginRequest != null && loginRequest.getPassword() != null
                ? loginRequest.getPassword()
                : "";

        // Chặn nhanh payload SQL Injection / ký tự điều khiển nguy hiểm trước khi truy vấn DB hoặc băm BCrypt
        if (identifier.isEmpty() || rawPassword.isEmpty()
                || identifier.contains("'") || identifier.contains("\"")
                || identifier.contains(";") || identifier.contains("--")
                || identifier.contains("/*") || identifier.contains("*/")) {
            throw new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            final Long failedUserId = user != null ? user.getId() : null;
            CompletableFuture.runAsync(() -> {
                try {
                    auditLogService.logFailure(failedUserId, identifier, "LOGIN_FAILED", "Auth", null, "Sai tên đăng nhập hoặc mật khẩu");
                } catch (Exception ex) {
                    log.warn("Async audit log failure failed: {}", ex.getMessage());
                }
            });
            throw new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            auditLogService.logFailure(user.getId(), user.getUsername(), "LOGIN_BLOCKED", "User", user.getId(), "Tài khoản của bạn đã bị khóa");
            throw new BadRequestException("Tài khoản của bạn đã bị khóa");
        }

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION || !user.isActive()) {
            throw new BadRequestException("Tài khoản chưa được kích hoạt. Vui lòng xác thực mã OTP đã gửi tới email: " + user.getEmail());
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        auditLogService.logSuccess(user.getId(), user.getUsername(), "LOGIN_SUCCESS", "User", user.getId(),
                "Đăng nhập thành công với vai trò " + user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        String normalizedUsername = registerRequest.getUsername() != null ? registerRequest.getUsername().trim() : "";
        String normalizedEmail = registerRequest.getEmail() != null ? registerRequest.getEmail().trim().toLowerCase() : "";

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BadRequestException("Username đã tồn tại trên hệ thống");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email đã được đăng ký trên hệ thống. Vui lòng đăng nhập hoặc sử dụng email khác.");
        }

        if (registerRequest.getPhoneNumber() != null && !registerRequest.getPhoneNumber().isBlank()) {
            if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber().trim())) {
                throw new BadRequestException("Số điện thoại đã được đăng ký trên hệ thống");
            }
        }

        Role role = registerRequest.getRole() != null ? registerRequest.getRole() : Role.ROLE_BUYER;

        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .phoneNumber(registerRequest.getPhoneNumber() != null ? registerRequest.getPhoneNumber().trim() : null)
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName() != null ? registerRequest.getFullName().trim() : normalizedUsername)
                .role(role)
                .status(UserStatus.PENDING_VERIFICATION)
                .active(false)
                .build();

        User savedUser = userRepository.save(user);

        // Sinh mã OTP 6 chữ số lưu vào Redis (TTL 5 phút, cooldown 60s)
        String otpCode = otpService.generateAndSaveOtp(savedUser.getEmail());

        // Gửi email chứa mã OTP xác thực
        emailService.sendOtpEmail(savedUser.getEmail(), savedUser.getFullName(), otpCode, "Xác thực đăng ký tài khoản");

        auditLogService.logSuccess(savedUser.getId(), savedUser.getUsername(), "REGISTER_PENDING_OTP", "User", savedUser.getId(),
                "Khởi tạo tài khoản thành công. Đã gửi mã xác thực OTP tới email: " + savedUser.getEmail());

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .status(savedUser.getStatus())
                .tokenType("Bearer")
                .expiresIn(300L)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request) {
        if (request == null || !StringUtils.hasText(request.getIdToken())) {
            throw new BadRequestException("Google ID Token hoặc Token xác thực không được để trống");
        }

        String token = request.getIdToken().trim();
        Map<String, Object> googleUser = verifyGoogleToken(token);

        if (googleUser == null || !googleUser.containsKey("email")) {
            throw new BadRequestException("Không thể xác thực danh tính với Google. Vui lòng thử lại!");
        }

        String email = ((String) googleUser.get("email")).toLowerCase().trim();
        String name = (String) googleUser.get("name");
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "LOGIN";

        User user = userRepository.findByEmail(email).orElse(null);

        // 1. Quên mật khẩu / Khôi phục tài khoản bằng Google
        if ("FORGOT_PASSWORD".equals(action)) {
            if (user == null) {
                throw new BadRequestException("Không tìm thấy tài khoản tương ứng với email Google: " + email + ". Vui lòng đăng ký tài khoản mới!");
            }
            if (!user.isActive() || user.getStatus() == UserStatus.BLOCKED) {
                throw new BadRequestException("Tài khoản của bạn đã bị khóa");
            }

            // Nếu người dùng cung cấp mật khẩu mới, cập nhật mật khẩu
            if (StringUtils.hasText(request.getNewPassword())) {
                user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
                user = userRepository.save(user);
            }

            // Kích hoạt nếu tài khoản đang ở trạng thái PENDING_VERIFICATION
            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                user.setStatus(UserStatus.ACTIVE);
                user.setActive(true);
                user = userRepository.save(user);
            }

            auditLogService.logSuccess(user.getId(), user.getUsername(), "FORGOT_PASSWORD_GOOGLE", "User", user.getId(),
                    "Khôi phục tài khoản thành công qua Google OAuth");

            return buildAuthResponse(user);
        }

        // 2. Đăng nhập / Đăng ký bằng Google
        if (user != null) {
            // Người dùng đã tồn tại -> Đăng nhập
            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                user.setStatus(UserStatus.ACTIVE);
                user.setActive(true);
                user = userRepository.save(user);
            }

            if (!user.isActive() || user.getStatus() == UserStatus.BLOCKED) {
                auditLogService.logFailure(user.getId(), user.getUsername(), "LOGIN_GOOGLE_BLOCKED", "User", user.getId(), "Tài khoản của bạn đã bị khóa");
                throw new BadRequestException("Tài khoản của bạn đã bị khóa");
            }

            auditLogService.logSuccess(user.getId(), user.getUsername(), "LOGIN_GOOGLE_SUCCESS", "User", user.getId(),
                    "Đăng nhập qua Google thành công [" + email + "]");

            return buildAuthResponse(user);
        } else {
            // Người dùng chưa tồn tại -> Tự động đăng ký tài khoản mới
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            if (baseUsername.length() < 3) baseUsername = "user_" + baseUsername;
            if (baseUsername.length() > 30) baseUsername = baseUsername.substring(0, 30);

            String finalUsername = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(finalUsername)) {
                finalUsername = baseUsername + "_" + counter++;
            }

            String fullName = StringUtils.hasText(name) ? name.trim() : finalUsername;

            User newUser = User.builder()
                    .username(finalUsername)
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Role.ROLE_BUYER)
                    .status(UserStatus.ACTIVE)
                    .active(true)
                    .build();

            newUser = userRepository.save(newUser);

            auditLogService.logSuccess(newUser.getId(), newUser.getUsername(), "REGISTER_GOOGLE_SUCCESS", "User", newUser.getId(),
                    "Đăng ký tài khoản mới qua Google thành công [" + email + "]");

            return buildAuthResponse(newUser);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleToken(String token) {
        // Cách 1: Thử verify qua Google TokenInfo (dành cho ID Token / JWT từ Google Credential)
        try {
            String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
            Map<String, Object> response = restTemplate.getForObject(tokenInfoUrl, Map.class);
            if (response != null && response.containsKey("email")) {
                return response;
            }
        } catch (Exception ex) {
            log.debug("Token is not an ID Token or tokeninfo failed: {}", ex.getMessage());
        }

        // Cách 2: Thử verify qua Google UserInfo (dành cho OAuth2 Access Token từ Google Popup)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().containsKey("email")) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception ex) {
            log.debug("UserInfo call failed: {}", ex.getMessage());
        }

        // Cách 3: Dự phòng giải mã Payload JWT nếu có 3 phần
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> claims = mapper.readValue(payload, Map.class);
                if (claims != null && claims.containsKey("email")) {
                    return claims;
                }
            }
        } catch (Exception ex) {
            log.warn("JWT payload decoding fallback failed: {}", ex.getMessage());
        }

        return null;
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        // Kiểm tra token đã bị thu hồi trong Redis chưa
        String revokedKey = REVOKED_TOKEN_PREFIX + refreshToken;
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(revokedKey))) {
                auditLogService.logFailure(null, "unknown", "TOKEN_REFRESH_REJECTED", "Token", null, "Sử dụng Refresh Token đã bị thu hồi");
                throw new BadRequestException("Refresh token này đã bị thu hồi hoặc đã đăng xuất trước đó");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Redis check failed for revoked token: {}", ex.getMessage());
        }

        String tokenType = tokenProvider.getTokenType(refreshToken);
        if (!"REFRESH".equalsIgnoreCase(tokenType)) {
            throw new BadRequestException("Token cung cấp không phải là refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!user.isActive() || user.getStatus() == UserStatus.BLOCKED) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        }

        // Token Rotation: Thu hồi refresh token cũ
        try {
            stringRedisTemplate.opsForValue().set(revokedKey, "ROTATED", Duration.ofDays(7));
        } catch (Exception ex) {
            log.warn("Failed to blacklist old refresh token: {}", ex.getMessage());
        }

        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        auditLogService.logSuccess(user.getId(), user.getUsername(), "TOKEN_REFRESH", "Token", user.getId(),
                "Làm mới Access Token & Refresh Token thành công (Token Rotation)");

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        User user = userRepository.findByEmail(verifyOtpRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email: " + verifyOtpRequest.getEmail()));

        if (user.getStatus() == UserStatus.ACTIVE && user.isActive()) {
            log.info("Tài khoản {} đã được kích hoạt trước đó", user.getEmail());
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400L)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .build();
        }

        // Xác thực mã OTP qua Redis
        otpService.verifyOtp(verifyOtpRequest.getEmail(), verifyOtpRequest.getOtp());

        // Kích hoạt tài khoản
        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        log.info("Kích hoạt tài khoản thành công cho user: {}", savedUser.getUsername());
        auditLogService.logSuccess(savedUser.getId(), savedUser.getUsername(), "OTP_VERIFY", "User", savedUser.getId(),
                "Xác thực mã OTP thành công, tài khoản đã được kích hoạt");

        String accessToken = tokenProvider.generateAccessToken(savedUser);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .status(savedUser.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OtpResponse resendOtp(ResendOtpRequest resendOtpRequest) {
        User user = userRepository.findByEmail(resendOtpRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email: " + resendOtpRequest.getEmail()));

        if (user.getStatus() == UserStatus.ACTIVE && user.isActive()) {
            throw new BadRequestException("Tài khoản này đã được kích hoạt thành công, không cần gửi lại OTP");
        }

        String otp = otpService.resendOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp, "Mã xác thực OTP mới");

        return OtpResponse.builder()
                .email(user.getEmail())
                .message("Mã OTP mới đã được gửi thành công tới email của bạn (hết hạn sau 5 phút)")
                .ttlSeconds(300L)
                .cooldownSeconds(60L)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OtpResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email: " + request.getEmail()));

        if (!user.isActive() || user.getStatus() == UserStatus.BLOCKED) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa");
        }

        String resetOtp = otpService.generateAndSaveResetOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), resetOtp, "Đặt lại mật khẩu");
        auditLogService.logSuccess(user.getId(), user.getUsername(), "PASSWORD_RESET_REQUEST", "User", user.getId(),
                "Yêu cầu cấp mã OTP đặt lại mật khẩu cho email: " + user.getEmail());

        return OtpResponse.builder()
                .email(user.getEmail())
                .message("Mã OTP đặt lại mật khẩu đã được gửi tới email của bạn (hết hạn sau 15 phút)")
                .ttlSeconds(900L)
                .cooldownSeconds(60L)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không trùng khớp");
        }

        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email: " + request.getEmail()));

        if (!user.isActive() || user.getStatus() == UserStatus.BLOCKED) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa");
        }

        // Xác thực OTP đặt lại mật khẩu
        otpService.verifyResetOtp(user.getEmail(), request.getOtp());

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Ghi Audit Log
        auditLogService.logSuccess(user.getId(), user.getUsername(), "PASSWORD_RESET", "User", user.getId(),
                "Đặt lại mật khẩu thành công qua mã OTP");

        log.info("Reset password successful for user: {} [{}]", user.getUsername(), user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    public void revokeToken(String token, Long userId) {
        if (StringUtils.hasText(token)) {
            try {
                stringRedisTemplate.opsForValue().set(REVOKED_TOKEN_PREFIX + token.trim(), "REVOKED", Duration.ofDays(7));
            } catch (Exception ex) {
                log.warn("Failed to blacklist revoked token in Redis: {}", ex.getMessage());
            }
        }
        String username = userId != null ? userRepository.findById(userId).map(User::getUsername).orElse("user_" + userId) : "anonymous";
        auditLogService.logSuccess(userId, username, "TOKEN_REVOKE", "Token", userId, "Thu hồi phiên đăng nhập / token thành công");
    }

    @Override
    public void logout(String token, Long userId) {
        revokeToken(token, userId);
        String username = userId != null ? userRepository.findById(userId).map(User::getUsername).orElse("user_" + userId) : "anonymous";
        auditLogService.logSuccess(userId, username, "LOGOUT", "User", userId, "Người dùng đã đăng xuất khỏi hệ thống");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .email("admin@quickshop.vn")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .role(Role.ROLE_ADMIN)
                    .status(UserStatus.ACTIVE)
                    .active(true)
                    .build());
            log.info("Initialized default admin account: admin / admin123");
        }

        if (!userRepository.existsByUsername("buyer")) {
            userRepository.save(User.builder()
                    .username("buyer")
                    .email("buyer@quickshop.vn")
                    .password(passwordEncoder.encode("buyer123"))
                    .fullName("Nguyễn Văn Mua")
                    .role(Role.ROLE_BUYER)
                    .status(UserStatus.ACTIVE)
                    .active(true)
                    .build());
            log.info("Initialized default buyer account: buyer / buyer123");
        }

        if (!userRepository.existsByUsername("customer")) {
            userRepository.save(User.builder()
                    .username("customer")
                    .email("customer@quickshop.vn")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Customer Demo")
                    .role(Role.ROLE_BUYER)
                    .status(UserStatus.ACTIVE)
                    .active(true)
                    .build());
            log.info("Initialized default customer alias account: customer / customer123");
        }

        if (!userRepository.existsByUsername("seller")) {
            User sellerUser = userRepository.save(User.builder()
                    .username("seller")
                    .email("seller@quickshop.vn")
                    .password(passwordEncoder.encode("seller123"))
                    .fullName("Trần Thị Bán Hàng")
                    .role(Role.ROLE_SELLER)
                    .status(UserStatus.ACTIVE)
                    .active(true)
                    .build());
            log.info("Initialized default seller account: seller / seller123");

            if (!shopRepository.existsByShopName("TechWorld Official Store")) {
                shopRepository.save(Shop.builder()
                        .ownerId(sellerUser.getId())
                        .shopName("TechWorld Official Store")
                        .taxCode("0102030405")
                        .phoneNumber("0988776655")
                        .bankCode("VCB")
                        .bankAccountNumber("1234567890")
                        .bankAccountName("TRAN THI BAN HANG")
                        .status(ShopStatus.ACTIVE)
                        .warehouseAddress("Kho Tổng Hoài Đức, Hà Nội")
                        .build());
                log.info("Initialized default active shop for seller: TechWorld Official Store");
            }
        }
    }
}
