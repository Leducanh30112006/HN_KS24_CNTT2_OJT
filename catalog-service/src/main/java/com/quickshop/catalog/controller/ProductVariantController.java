package com.quickshop.catalog.controller;

import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.service.ProductVariantService;
import com.quickshop.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Product Variant & SKU API", description = "Quản lý biến thể SKU, giá bán, tồn kho thực tế và reserved_stock; kiểm tra không cho tồn kho âm")
public class ProductVariantController {

    private final ProductVariantService variantService;

    @GetMapping("/products/{productId}/variants")
    @Operation(summary = "Lấy danh sách tất cả biến thể SKU của sản phẩm")
    public ResponseEntity<ApiResponse<List<VariantDto>>> getVariantsByProduct(@PathVariable Long productId) {
        List<VariantDto> list = variantService.getVariantsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách biến thể thành công", list));
    }

    @GetMapping("/variants/{id}")
    @Operation(summary = "Lấy chi tiết biến thể theo ID")
    public ResponseEntity<ApiResponse<VariantDto>> getVariantById(@PathVariable Long id) {
        VariantDto dto = variantService.getVariantById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết biến thể thành công", dto));
    }

    @GetMapping("/variants/sku/{sku}")
    @Operation(summary = "Tra cứu biến thể theo mã SKU")
    public ResponseEntity<ApiResponse<VariantDto>> getVariantBySku(@PathVariable String sku) {
        VariantDto dto = variantService.getVariantBySku(sku);
        return ResponseEntity.ok(ApiResponse.success("Tra cứu biến thể theo mã SKU thành công", dto));
    }

    @PostMapping("/products/{productId}/variants")
    @Operation(summary = "Thêm biến thể SKU mới cho sản phẩm (Kiểm tra quyền sở hữu Shop, giá và tồn kho không âm)")
    public ResponseEntity<ApiResponse<VariantDto>> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody VariantCreateRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        VariantDto created = variantService.addVariant(productId, request, callerShopId, userRoleHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm biến thể SKU thành công", created));
    }

    @PutMapping("/variants/{id}")
    @Operation(summary = "Cập nhật đầy đủ thông tin biến thể SKU (Màu sắc, kích cỡ, SKU, giá, tồn kho)")
    public ResponseEntity<ApiResponse<VariantDto>> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody VariantUpdateRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        VariantDto updated = variantService.updateVariant(id, request, callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật biến thể thành công", updated));
    }

    @PatchMapping("/variants/{id}/price")
    @Operation(summary = "Cập nhật giá bán biến thể SKU (Kiểm tra giá >= 0)")
    public ResponseEntity<ApiResponse<VariantDto>> updatePrice(
            @PathVariable Long id,
            @Valid @RequestBody VariantPriceUpdateRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        VariantDto updated = variantService.updatePrice(id, request.getPrice(), callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giá bán biến thể thành công", updated));
    }

    @PatchMapping("/variants/{id}/stock")
    @Operation(summary = "Điều chỉnh tồn kho biến thể SKU (Kiểm tra không cho tồn kho âm)")
    public ResponseEntity<ApiResponse<VariantDto>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody VariantStockUpdateRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        VariantDto updated = variantService.updateStock(id, request, callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh tồn kho biến thể thành công", updated));
    }

    @DeleteMapping("/variants/{id}")
    @Operation(summary = "Xóa biến thể SKU (Không cho xóa biến thể duy nhất còn lại)")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable Long id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader
    ) {
        Long callerShopId = extractShopId(shopIdHeader, userIdHeader);
        variantService.deleteVariant(id, callerShopId, userRoleHeader);
        return ResponseEntity.ok(ApiResponse.success("Xóa biến thể SKU thành công", null));
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
