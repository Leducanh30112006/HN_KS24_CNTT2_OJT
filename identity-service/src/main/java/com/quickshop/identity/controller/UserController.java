package com.quickshop.identity.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Quản lý hồ sơ người dùng và danh sách tài khoản")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả người dùng (Dành cho Admin)")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userRepository.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin người dùng theo ID")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<User>> getMyProfile(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            throw new BadRequestException("Yêu cầu chưa được xác thực danh tính");
        }
        User user = userRepository.findById(Long.parseLong(userIdHeader))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật hồ sơ cá nhân")
    public ResponseEntity<ApiResponse<User>> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestBody UpdateProfileRequest request
    ) {
        if (!StringUtils.hasText(userIdHeader)) {
            throw new BadRequestException("Yêu cầu chưa được xác thực danh tính");
        }
        User user = userRepository.findById(Long.parseLong(userIdHeader))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userIdHeader));

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User updated = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hồ sơ thành công", updated));
    }

    @Data
    public static class UpdateProfileRequest {
        private String fullName;
        private String phoneNumber;
    }
}
