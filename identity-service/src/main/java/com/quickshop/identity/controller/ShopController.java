package com.quickshop.identity.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.dto.CreateShopRequest;
import com.quickshop.identity.dto.ShopResponse;
import com.quickshop.identity.dto.UpdateShopRequest;
import com.quickshop.identity.entity.ShopStatus;
import com.quickshop.identity.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Tag(name = "Shop & Seller Management", description = "Quản lý gian hàng, đăng ký Seller KYC Onboarding và duyệt mở Shop")
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả gian hàng (Public)")
    public ResponseEntity<ApiResponse<List<ShopResponse>>> getAllShops() {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getAllShops()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết gian hàng theo ID (Public)")
    public ResponseEntity<ApiResponse<ShopResponse>> getShopById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getShopById(id)));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Lấy gian hàng theo ID chủ sở hữu (Public)")
    public ResponseEntity<ApiResponse<ShopResponse>> getShopByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getShopByOwnerId(ownerId)));
    }

    @GetMapping("/my-shop")
    @Operation(summary = "Lấy thông tin gian hàng của tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long ownerId = parseUserId(userIdHeader);
        return ResponseEntity.ok(ApiResponse.ok(shopService.getMyShop(ownerId)));
    }

    @PostMapping("/register")
    @Operation(summary = "Người dùng đăng ký mở gian hàng (Onboarding KYC - Trạng thái PENDING_APPROVAL)")
    public ResponseEntity<ApiResponse<ShopResponse>> registerShop(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody CreateShopRequest request
    ) {
        Long ownerId = parseUserId(userIdHeader);
        ShopResponse response = shopService.registerShop(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Hồ sơ đăng ký mở gian hàng đã được gửi thành công và đang chờ Admin duyệt", response));
    }

    @PutMapping("/my-shop")
    @Operation(summary = "Chủ gian hàng cập nhật thông tin hồ sơ gian hàng cá nhân")
    public ResponseEntity<ApiResponse<ShopResponse>> updateMyShop(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody UpdateShopRequest request
    ) {
        Long ownerId = parseUserId(userIdHeader);
        ShopResponse myShop = shopService.getMyShop(ownerId);
        ShopResponse response = shopService.updateShop(ownerId, myShop.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin gian hàng thành công", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin gian hàng theo ID")
    public ResponseEntity<ApiResponse<ShopResponse>> updateShopById(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable Long id,
            @Valid @RequestBody UpdateShopRequest request
    ) {
        Long ownerId = StringUtils.hasText(userIdHeader) ? Long.parseLong(userIdHeader) : null;
        ShopResponse response = shopService.updateShop(ownerId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin gian hàng thành công", response));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lọc danh sách gian hàng theo trạng thái (PENDING_APPROVAL, ACTIVE, REJECTED, SUSPENDED)")
    public ResponseEntity<ApiResponse<List<ShopResponse>>> getShopsByStatus(@PathVariable ShopStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getShopsByStatus(status)));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Admin duyệt mở gian hàng & nâng quyền ROLE_SELLER cho chủ shop")
    public ResponseEntity<ApiResponse<ShopResponse>> approveShop(@PathVariable Long id) {
        ShopResponse response = shopService.approveShop(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã phê duyệt gian hàng thành công và cấp quyền ROLE_SELLER", response));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Admin từ chối hồ sơ mở gian hàng")
    public ResponseEntity<ApiResponse<ShopResponse>> rejectShop(@PathVariable Long id) {
        ShopResponse response = shopService.rejectShop(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối hồ sơ mở gian hàng", response));
    }

    private Long parseUserId(String userIdHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            throw new BadRequestException("Yêu cầu chưa được xác thực danh tính người dùng");
        }
        try {
            return Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Định dạng User ID không hợp lệ: " + userIdHeader);
        }
    }
}
