package com.quickshop.catalog.dto;

import com.quickshop.catalog.entity.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    private Long shopId;

    @NotNull(message = "Danh mục sản phẩm không được để trống")
    private Long categoryId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;
    private String mainImage;

    /**
     * Cờ đánh dấu tạo bản nháp (DRAFT).
     * Nếu true: sản phẩm lưu ở trạng thái DRAFT.
     * Nếu false hoặc null: sản phẩm tạo ở trạng thái PENDING để chờ duyệt.
     */
    private Boolean isDraft;

    /**
     * Hoặc truyền trực tiếp trạng thái DRAFT / PENDING
     */
    private ProductStatus status;

    @NotEmpty(message = "Sản phẩm phải có ít nhất một biến thể SKU")
    @Valid
    private List<VariantCreateRequest> variants;
}
