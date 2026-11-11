package com.quickshop.catalog.controller;

import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.service.ProductReviewService;
import com.quickshop.catalog.service.ProductService;
import com.quickshop.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Product Catalog API", description = "Tìm kiếm đa tiêu chí, CRUD Sản phẩm theo Shop, Quản lý trạng thái DRAFT/PENDING/APPROVED/REJECTED và Phân quyền")
public class ProductController {

    private final ProductService productService;
    private final ProductReviewService reviewService;

    @GetMapping("/products")
    @Operation(summary = "Tìm kiếm & Lọc sản phẩm Storefront (Cached Redis TTL 10m - Mặc định chỉ hiển thị APPROVED)")
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
        PageResult<ProductDto> result = productService.getProducts(
                keyword, categoryId, shopId, status, minPrice, maxPrice, minRating, page, size, sortBy
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", result));
    }

    @GetMapping("/products/admin")
    @Operation(summary = "Admin lấy danh sách sản phẩm mọi trạng thái kiểm duyệt (Không cache - Yêu cầu ROLE_ADMIN)")
    public ResponseEntity<ApiResponse<PageResult<ProductDto>>> getProductsForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        PageResult<ProductDto> result = productService.getProductsForAdmin(
                keyword, categoryId, shopId, status, page, size, userRoleHeader
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm kiểm duyệt thành công", result));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Chi tiết sản phẩm, danh sách biến thể SKU & số sao đánh giá (Kiểm tra quyền nếu chưa APPROVED)")
    public ResponseEntity<ApiResponse<ProductDto>> getProductDetails(
            @PathVariable Long id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        ProductDto dto = productService.getProductById(id, callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", dto));
    }

    @GetMapping("/products/seller/my")
    @Operation(summary = "Seller lấy danh sách sản phẩm theo Shop (hỗ trợ lọc theo trạng thái DRAFT, PENDING, APPROVED, REJECTED)")
    public ResponseEntity<ApiResponse<PageResult<ProductDto>>> getMySellerProducts(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        Long targetShopId = shopId != null ? shopId : (callerShopId != null ? callerShopId : 1L);

        PageResult<ProductDto> result = productService.getProductsByShop(targetShopId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm của Shop thành công", result));
    }

    @PostMapping("/products")
    @Operation(summary = "Seller tạo sản phẩm mới (Mặc định DRAFT hoặc PENDING, kiểm tra quyền)")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        ProductDto created = productService.createProduct(request, callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.created("Tạo sản phẩm thành công (Trạng thái: " + created.getStatus() + ")", created));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Cập nhật sản phẩm theo Shop (Kiểm tra quyền sở hữu Shop)")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        ProductDto updated = productService.updateProduct(id, request, callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin sản phẩm thành công", updated));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Xóa sản phẩm theo Shop (Kiểm tra quyền sở hữu Shop)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        productService.deleteProduct(id, callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    @PostMapping("/products/{id}/submit")
    @Operation(summary = "Seller nộp duyệt sản phẩm: Chuyển từ DRAFT hoặc REJECTED sang PENDING")
    public ResponseEntity<ApiResponse<ProductDto>> submitProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        ProductDto submitted = productService.submitForApproval(id, callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Nộp kiểm duyệt sản phẩm thành công (Trạng thái: PENDING)", submitted));
    }

    @PatchMapping("/products/{id}/status")
    @Operation(summary = "Admin kiểm duyệt sản phẩm: Phê duyệt APPROVED hoặc Từ chối REJECTED (Yêu cầu ROLE_ADMIN)")
    public ResponseEntity<ApiResponse<ProductDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusUpdateRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        ProductDto updated = productService.updateProductStatus(id, request.getStatus(), request.getReason(), userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái kiểm duyệt sản phẩm thành công", updated));
    }

    @GetMapping("/variants/{variantId}/validation")
    @Operation(summary = "Xác thực biến thể SKU, trạng thái Shop và Tồn kho khả dụng (Contract API cho Order Service)")
    public ResponseEntity<ApiResponse<CatalogVariantValidationDto>> getVariantValidation(
            @PathVariable Long variantId
    ) {
        CatalogVariantValidationDto validation = productService.getVariantValidation(variantId);
        return ResponseEntity.ok(ApiResponse.success("Xác thực biến thể thành công", validation));
    }

    @GetMapping("/products/{id}/reviews")
    @Operation(summary = "Lấy danh sách đánh giá & nhận xét của sản phẩm")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getProductReviews(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy đánh giá sản phẩm thành công", reviewService.getReviewsByProduct(id)));
    }

    @PostMapping("/products/{id}/reviews")
    @Operation(summary = "Khách hàng gửi đánh giá 1-5 sao và nhận xét sau khi mua hàng")
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

    private Long extractShopId(String shopIdHeader, String userIdHeader) {
        if (shopIdHeader != null && !shopIdHeader.isBlank()) {
            try {
                return Long.parseLong(shopIdHeader.trim());
            } catch (NumberFormatException ignored) {}
        }
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
