package com.quickshop.identity.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.identity.entity.Shop;
import com.quickshop.identity.entity.ShopStatus;
import com.quickshop.identity.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopRepository shopRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Shop>>> getAllShops() {
        return ResponseEntity.ok(ApiResponse.success(shopRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Shop>> getShopById(@PathVariable Long id) {
        Optional<Shop> shopOpt = shopRepository.findById(id);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(shopOpt.get()));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiResponse<Shop>> getShopByOwner(@PathVariable Long ownerId) {
        Optional<Shop> shopOpt = shopRepository.findByOwnerId(ownerId);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(shopOpt.get()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Shop>> registerShop(@RequestBody Shop shopRequest, @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            shopRequest.setOwnerId(Long.parseLong(userIdHeader));
        }
        shopRequest.setStatus(ShopStatus.PENDING_APPROVAL);
        Shop savedShop = shopRepository.save(shopRequest);
        return ResponseEntity.ok(ApiResponse.success("Hồ sơ đăng ký mở gian hàng đã được gửi và đang chờ Admin duyệt", savedShop));
    }
}
