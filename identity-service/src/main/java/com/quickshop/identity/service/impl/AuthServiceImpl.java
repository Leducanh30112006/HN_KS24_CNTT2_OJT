package com.quickshop.identity.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.dto.AuthResponse;
import com.quickshop.identity.dto.LoginRequest;
import com.quickshop.identity.dto.RegisterRequest;
import com.quickshop.identity.entity.Role;
import com.quickshop.identity.entity.Shop;
import com.quickshop.identity.entity.ShopStatus;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.entity.UserStatus;
import com.quickshop.identity.repository.ShopRepository;
import com.quickshop.identity.repository.UserRepository;
import com.quickshop.identity.security.JwtTokenProvider;
import com.quickshop.identity.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail())
                .orElseThrow(() -> new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (!user.isActive() || user.getStatus() == UserStatus.BLOCKED) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa");
        }

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
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BadRequestException("Username đã tồn tại trên hệ thống");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email đã được đăng ký trên hệ thống");
        }

        Role role = registerRequest.getRole() != null ? registerRequest.getRole() : Role.ROLE_BUYER;

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .role(role)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

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
                .build();
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
