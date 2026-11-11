package com.quickshop.notification.ai.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Yêu cầu tìm kiếm sản phẩm từ AI Function Calling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tham số gọi tìm kiếm Catalog từ AI Shopping Assistant")
public class AiCatalogSearchRequest {

    @Schema(description = "Từ khóa tìm kiếm (đã được làm sạch)", example = "laptop lập trình")
    private String keyword;

    @Schema(description = "ID danh mục ngành hàng (nếu có)", example = "5")
    private Long categoryId;

    @Schema(description = "Khoảng giá tối thiểu", example = "10000000")
    private BigDecimal minPrice;

    @Schema(description = "Khoảng giá tối đa", example = "50000000")
    private BigDecimal maxPrice;

    @Builder.Default
    @Schema(description = "Số lượng sản phẩm tối đa lấy về phục vụ prompt", example = "5")
    private int limit = 5;

    @Schema(description = "Vai trò người dùng gọi API (kiểm soát phân quyền)", example = "ROLE_BUYER")
    private String userRole;
}
