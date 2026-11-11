package com.quickshop.notification.ai.catalog;

import java.util.List;
import java.util.Map;

/**
 * Interface dịch vụ tìm kiếm Catalog phục vụ AI Shopping Assistant
 * Tích hợp Circuit Breaker, Timeout, Fallback và Kiểm soát quyền truy cập dữ liệu.
 */
public interface AiCatalogSearchService {

    /**
     * Tìm kiếm sản phẩm theo từ khóa và tiêu chí lọc
     */
    AiCatalogSearchResult searchProducts(AiCatalogSearchRequest request);

    /**
     * Lấy danh sách sản phẩm mẫu dự phòng an toàn (Curated Fallback Catalog)
     */
    List<AiCatalogItemDto> getFallbackItems(String keyword);

    /**
     * Lấy đối tượng Circuit Breaker giám sát
     */
    AiCatalogCircuitBreaker getCircuitBreaker();

    /**
     * Tra cứu thông tin trạng thái Circuit Breaker
     */
    Map<String, Object> getCircuitBreakerStatus();

    /**
     * Đặt lại trạng thái Circuit Breaker
     */
    void resetCircuitBreaker();
}
