package com.quickshop.catalog.controller;

import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.service.ProductReviewService;
import com.quickshop.catalog.service.ProductService;
import com.quickshop.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog API", description = "Tìm kiếm đa tiêu chí, CRUD Sản phẩm, Biến thể SKU, Kiểm duyệt và Đánh giá (Redis Caching)")
public class ProductController {

    private final ProductService productService;
    private final ProductReviewService reviewService;

    @GetMapping
    @Operation(summary = "Tìm kiếm & Lọc sản phẩm đa tiêu chí (Cached Redis TTL 10m)")
    public ResponseEntity<ApiResponse<PageResult<ProductDto>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy
    ) {
        // Mặc định đối với khách mua hàng Storefront chỉ hiển thị ACTIVE nếu không chỉ định status
        ProductStatus queryStatus = status != null ? status : ProductStatus.ACTIVE;

        PageResult<ProductDto> result = productService.getProducts(
                keyword, categoryId, shopId, queryStatus, minPrice, maxPrice, minRating, page, size, sortBy
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sản phẩm, danh sách biến thể SKU & số sao đánh giá (Cached Redis)")
    public ResponseEntity<ApiResponse<ProductDto>> getProductDetails(@PathVariable Long id) {
        ProductDto dto = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", dto));
    }

    @GetMapping("/seller/my")
    @Operation(summary = "Seller lấy danh sách sản phẩm theo Shop (kèm trạng thái duyệt)")
    public ResponseEntity<ApiResponse<PageResult<ProductDto>>> getMySellerProducts(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long targetShopId = shopId != null ? shopId : 1L;
        PageResult<ProductDto> result = productService.getProductsByShop(targetShopId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm của Shop thành công", result));
    }

    @PostMapping
    @Operation(summary = "Seller đăng bán sản phẩm mới với biến thể SKU (Trạng thái ban đầu: PENDING_APPROVAL)")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long authShopId = null;
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                authShopId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException ignored) {}
        }

        ProductDto created = productService.createProduct(request, authShopId);
        return ResponseEntity.ok(ApiResponse.created("Đăng bán sản phẩm thành công, đang chờ Admin duyệt", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin sản phẩm (Tự động xóa Redis cache)")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long shopId = null;
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                shopId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException ignored) {}
        }

        ProductDto updated = productService.updateProduct(id, request, shopId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin sản phẩm thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa sản phẩm (Tự động xóa Redis cache)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long shopId = null;
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                shopId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException ignored) {}
        }

        productService.deleteProduct(id, shopId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Admin kiểm duyệt sản phẩm: Duyệt ACTIVE hoặc Từ chối REJECTED")
    public ResponseEntity<ApiResponse<ProductDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request
    ) {
        ProductDto updated = productService.updateProductStatus(id, request.getStatus(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái kiểm duyệt thành công", updated));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Lấy danh sách đánh giá & nhận xét của sản phẩm (FR-04.1)")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getProductReviews(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy đánh giá sản phẩm thành công", reviewService.getReviewsByProduct(id)));
    }

    @PostMapping("/{id}/reviews")
    @Operation(summary = "Khách hàng gửi đánh giá 1-5 sao và nhận xét sau khi mua hàng (FR-04.1)")
    public ResponseEntity<ApiResponse<ReviewDto>> addProductReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Auth-Username", required = false) String usernameHeader
    ) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                request.setUserId(Long.parseLong(userIdHeader));
            } catch (NumberFormatException ignored) {}
        }
        if (usernameHeader != null && !usernameHeader.isBlank()) {
            request.setUserName(usernameHeader);
        }

        ReviewDto saved = reviewService.addReview(id, request);
        return ResponseEntity.ok(ApiResponse.created("Đánh giá sản phẩm thành công", saved));
    }

    @Data
    public static class StatusUpdateRequest {
        private ProductStatus status;
        private String reason;
    }
}
