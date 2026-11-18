package com.quickshop.catalog.controller;

import com.quickshop.catalog.dto.CategoryDto;
import com.quickshop.catalog.exception.ForbiddenException;
import com.quickshop.catalog.service.CategoryService;
import com.quickshop.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Quản lý cây danh mục sàn QuickShop (Hỗ trợ Redis Cache TTL 10m)")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả danh mục (có đếm số sản phẩm & cache Redis)")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục thành công", categoryService.getAllCategories()));
    }

    @GetMapping("/tree")
    @Operation(summary = "Lấy cây danh mục phân cấp Cha - Con (Category Tree - Redis Cache TTL 10m)")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategoryTree() {
        return ResponseEntity.ok(ApiResponse.success("Lấy cây danh mục thành công", categoryService.getCategoryTree()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết một danh mục")
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    @PostMapping
    @Operation(summary = "Admin tạo danh mục mới (tự động xóa cache)")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @RequestBody CategoryDto dto,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        requireAdmin(userRole);
        CategoryDto created = categoryService.createCategory(dto);
        return ResponseEntity.ok(ApiResponse.created("Tạo danh mục mới thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Admin cập nhật danh mục (tự động xóa cache)")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryDto dto,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        requireAdmin(userRole);
        CategoryDto updated = categoryService.updateCategory(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Admin xóa danh mục (tự động xóa cache)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        requireAdmin(userRole);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }

    private void requireAdmin(String userRole) {
        if (userRole == null || (!"ROLE_ADMIN".equalsIgnoreCase(userRole.trim()) && !"ADMIN".equalsIgnoreCase(userRole.trim()))) {
            throw new ForbiddenException("Bạn không có quyền quản trị danh mục (Yêu cầu ROLE_ADMIN)");
        }
    }
}
