package com.quickshop.catalog.controller;

import com.quickshop.catalog.dto.CategoryDto;
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

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết một danh mục")
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    @PostMapping
    @Operation(summary = "Admin tạo danh mục mới (tự động xóa cache)")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(@RequestBody CategoryDto dto) {
        CategoryDto created = categoryService.createCategory(dto);
        return ResponseEntity.ok(ApiResponse.created("Tạo danh mục mới thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Admin cập nhật danh mục (tự động xóa cache)")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryDto dto
    ) {
        CategoryDto updated = categoryService.updateCategory(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Admin xóa danh mục (tự động xóa cache)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }
}
