package com.quickshop.identity.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.identity.entity.Role;
import com.quickshop.identity.entity.Shop;
import com.quickshop.identity.entity.ShopStatus;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.repository.ShopRepository;
import com.quickshop.identity.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Tag(name = "Shop & Seller Management", description = "Quản lý gian hàng, đăng ký Seller KYC và duyệt mở Shop")
public class ShopController {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả gian hàng")
    public ResponseEntity<ApiResponse<List<Shop>>> getAllShops() {
        return ResponseEntity.ok(ApiResponse.success(shopRepository.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết gian hàng theo ID")
    public ResponseEntity<ApiResponse<Shop>> getShopById(@PathVariable Long id) {
        Optional<Shop> shopOpt = shopRepository.findById(id);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(shopOpt.get()));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Lấy gian hàng theo ID chủ sở hữu")
    public ResponseEntity<ApiResponse<Shop>> getShopByOwner(@PathVariable Long ownerId) {
        Optional<Shop> shopOpt = shopRepository.findByOwnerId(ownerId);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(shopOpt.get()));
    }

    @PostMapping("/register")
    @Operation(summary = "Người dùng đăng ký mở gian hàng (Onboarding KYC)")
    public ResponseEntity<ApiResponse<Shop>> registerShop(
            @RequestBody Shop shopRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            shopRequest.setOwnerId(Long.parseLong(userIdHeader));
        }
        shopRequest.setStatus(ShopStatus.PENDING_APPROVAL);
        Shop savedShop = shopRepository.save(shopRequest);
        return ResponseEntity.ok(ApiResponse.success("Hồ sơ đăng ký mở gian hàng đã được gửi và đang chờ Admin duyệt", savedShop));
    }

    @PutMapping("/{id}/approve")
    @Transactional
    @Operation(summary = "Admin duyệt mở gian hàng & nâng quyền ROLE_SELLER cho chủ shop")
    public ResponseEntity<ApiResponse<Shop>> approveShop(@PathVariable Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));

        shop.setStatus(ShopStatus.ACTIVE);
        Shop updatedShop = shopRepository.save(shop);

        // Nâng quyền tài khoản chủ shop thành ROLE_SELLER
        userRepository.findById(shop.getOwnerId()).ifPresent(user -> {
            user.setRole(Role.ROLE_SELLER);
            userRepository.save(user);
        });

        return ResponseEntity.ok(ApiResponse.success("Đã phê duyệt gian hàng thành công và cấp quyền ROLE_SELLER", updatedShop));
    }

    @PutMapping("/{id}/reject")
    @Transactional
    @Operation(summary = "Admin từ chối hồ sơ mở gian hàng")
    public ResponseEntity<ApiResponse<Shop>> rejectShop(@PathVariable Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));

        shop.setStatus(ShopStatus.REJECTED);
        Shop updatedShop = shopRepository.save(shop);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối hồ sơ mở gian hàng", updatedShop));
    }
}
