package com.quickshop.identity.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.dto.AddressRequest;
import com.quickshop.identity.dto.AddressResponse;
import com.quickshop.identity.security.JwtTokenProvider;
import com.quickshop.identity.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/users/addresses", "/api/users/me/addresses"})
@RequiredArgsConstructor
@Tag(name = "User Address Management", description = "Quản lý sổ địa chỉ giao hàng của người dùng, tích hợp định vị bản đồ Leaflet OpenStreetMap")
public class UserAddressController {

    private final UserAddressService userAddressService;
    private final JwtTokenProvider jwtTokenProvider;

    private Long resolveUserId(String userIdHeader, String authHeader) {
        if (StringUtils.hasText(userIdHeader)) {
            try {
                return Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException e) {
                log.warn("Giá trị X-User-Id không hợp lệ: {}", userIdHeader);
            }
        }

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtTokenProvider.validateToken(token)) {
                return jwtTokenProvider.getUserIdFromToken(token);
            }
        }

        throw new BadRequestException("Yêu cầu chưa được xác thực danh tính người dùng");
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách địa chỉ của người dùng hiện tại")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = resolveUserId(userIdHeader, authHeader);
        List<AddressResponse> addresses = userAddressService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.ok(addresses));
    }

    @GetMapping("/default")
    @Operation(summary = "Lấy địa chỉ giao hàng mặc định của người dùng")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = resolveUserId(userIdHeader, authHeader);
        AddressResponse address = userAddressService.getDefaultAddress(userId);
        return ResponseEntity.ok(ApiResponse.ok(address));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết một địa chỉ theo ID")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = resolveUserId(userIdHeader, authHeader);
        AddressResponse address = userAddressService.getAddressById(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(address));
    }

    @PostMapping
    @Operation(summary = "Thêm mới địa chỉ giao hàng (kèm tọa độ GPS từ bản đồ)")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @Valid @RequestBody AddressRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = resolveUserId(userIdHeader, authHeader);
        AddressResponse created = userAddressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm địa chỉ giao hàng thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật địa chỉ giao hàng (kèm tọa độ GPS từ bản đồ)")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = resolveUserId(userIdHeader, authHeader);
        AddressResponse updated = userAddressService.updateAddress(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật địa chỉ thành công", updated));
    }

    @RequestMapping(value = "/{id}/default", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @Operation(summary = "Đặt địa chỉ làm mặc định (hỗ trợ cả PATCH và PUT)")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = resolveUserId(userIdHeader, authHeader);
        AddressResponse updated = userAddressService.setDefaultAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Đã đặt làm địa chỉ mặc định", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa địa chỉ giao hàng")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = resolveUserId(userIdHeader, authHeader);
        userAddressService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa địa chỉ thành công", null));
    }
}
