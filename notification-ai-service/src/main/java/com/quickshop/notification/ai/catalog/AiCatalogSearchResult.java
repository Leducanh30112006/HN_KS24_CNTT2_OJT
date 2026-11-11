package com.quickshop.notification.ai.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Kết quả tìm kiếm dữ liệu Catalog phục vụ AI Prompt Context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả tìm kiếm Catalog cho AI Prompt")
public class AiCatalogSearchResult implements Serializable {

    @Schema(description = "Danh sách sản phẩm tìm thấy")
    @Builder.Default
    private List<AiCatalogItemDto> items = Collections.emptyList();

    @Schema(description = "Tổng số sản phẩm tìm thấy")
    private int totalFound;

    @Schema(description = "Danh sách các đoạn tóm tắt (Snippets) đưa vào ngữ cảnh AI")
    @Builder.Default
    private List<String> snippets = Collections.emptyList();

    @Schema(description = "Nguồn dữ liệu (CATALOG_SERVICE / FALLBACK_CACHE / CIRCUIT_BREAKER_FALLBACK)")
    private String source;

    @Schema(description = "Có sử dụng dữ liệu dự phòng hay không")
    private boolean fallbackUsed;

    @Schema(description = "Trạng thái hiện tại của Circuit Breaker")
    private CircuitBreakerState circuitBreakerState;

    @Schema(description = "Thời gian thực thi tìm kiếm (ms)")
    private long executionTimeMs;
}
