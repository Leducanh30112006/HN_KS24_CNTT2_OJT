package com.quickshop.notification.ai.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Tests - AI Catalog Function, Circuit Breaker & Data Access Validator")
class AiCatalogSearchServiceTest {

    private AiCatalogAccessValidator accessValidator;
    private AiCatalogCircuitBreaker circuitBreaker;
    private AiCatalogSearchServiceImpl catalogSearchService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        accessValidator = new AiCatalogAccessValidator();
        circuitBreaker = new AiCatalogCircuitBreaker(3, 1000L);
        objectMapper = new ObjectMapper();

        // Khởi tạo service trỏ vào URL mock
        catalogSearchService = new AiCatalogSearchServiceImpl(
                "http://localhost:18082",
                2000,
                3000,
                circuitBreaker,
                accessValidator,
                objectMapper
        );
    }

    @Test
    @DisplayName("Access Validator - Làm sạch từ khóa tìm kiếm chống SQL Injection và XSS")
    void testKeywordSanitization() {
        String dirtyInput = "'; DROP TABLE products; -- <script>alert('xss')</script> Laptop Dell ";
        String sanitized = accessValidator.sanitizeKeyword(dirtyInput);

        assertThat(sanitized).doesNotContain("DROP TABLE");
        assertThat(sanitized).doesNotContain("<script>");
        assertThat(sanitized).doesNotContain("alert");
        assertThat(sanitized).doesNotContain(";");
        assertThat(sanitized).contains("Laptop Dell");
    }

    @Test
    @DisplayName("Access Validator - Lọc trạng thái sản phẩm: Chỉ cho phép ACTIVE và APPROVED")
    void testProductVisibilityFiltering() {
        AiCatalogItemDto activeItem = AiCatalogItemDto.builder()
                .id(1L)
                .name("Sản phẩm Active")
                .status("ACTIVE")
                .build();

        AiCatalogItemDto approvedItem = AiCatalogItemDto.builder()
                .id(2L)
                .name("Sản phẩm Approved")
                .status("APPROVED")
                .build();

        AiCatalogItemDto draftItem = AiCatalogItemDto.builder()
                .id(3L)
                .name("Sản phẩm Nháp")
                .status("DRAFT")
                .build();

        AiCatalogItemDto rejectedItem = AiCatalogItemDto.builder()
                .id(4L)
                .name("Sản phẩm Bị từ chối")
                .status("REJECTED")
                .build();

        assertThat(accessValidator.isProductVisibleForBuyer(activeItem)).isTrue();
        assertThat(accessValidator.isProductVisibleForBuyer(approvedItem)).isTrue();
        assertThat(accessValidator.isProductVisibleForBuyer(draftItem)).isFalse();
        assertThat(accessValidator.isProductVisibleForBuyer(rejectedItem)).isFalse();

        List<AiCatalogItemDto> filtered = accessValidator.filterVisibleProductsForBuyer(
                List.of(activeItem, approvedItem, draftItem, rejectedItem)
        );

        assertThat(filtered).hasSize(2);
        assertThat(filtered).extracting("id").containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Circuit Breaker - Chuyển đổi trạng thái CLOSED -> OPEN sau 3 lần thất bại liên tiếp")
    void testCircuitBreakerTransitions() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(circuitBreaker.allowExecution()).isTrue();

        // 1st failure
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(1);

        // 2nd failure
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(2);

        // 3rd failure -> trips to OPEN
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(circuitBreaker.allowExecution()).isFalse();

        // Reset về CLOSED
        circuitBreaker.reset();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(0);
        assertThat(circuitBreaker.allowExecution()).isTrue();
    }

    @Test
    @DisplayName("Circuit Breaker - Tự động thử nghiệm HALF_OPEN sau khi hết thời gian reset timeout")
    void testCircuitBreakerHalfOpen() throws InterruptedException {
        // Circuit breaker với reset timeout siêu ngắn 50ms để test
        AiCatalogCircuitBreaker fastBreaker = new AiCatalogCircuitBreaker(2, 50L);
        fastBreaker.recordFailure();
        fastBreaker.recordFailure();

        assertThat(fastBreaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(fastBreaker.allowExecution()).isFalse();

        // Chờ 60ms cho timeout trôi qua
        Thread.sleep(60);

        // Lúc này sẽ chuyển sang HALF_OPEN và cho phép 1 request thử nghiệm
        assertThat(fastBreaker.allowExecution()).isTrue();
        assertThat(fastBreaker.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);

        // Nếu request thành công -> trở lại CLOSED
        fastBreaker.recordSuccess();
        assertThat(fastBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(fastBreaker.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("AiCatalogSearchService - Fallback sang Curated Knowledge Base khi Catalog Service ngắt kết nối")
    void testSearchProductsFallbackOnNetworkError() {
        // Cố tình gọi đến port không tồn tại để kích hoạt lỗi kết nối
        AiCatalogSearchRequest request = AiCatalogSearchRequest.builder()
                .keyword("laptop")
                .limit(5)
                .build();

        AiCatalogSearchResult result = catalogSearchService.searchProducts(request);

        assertThat(result).isNotNull();
        assertThat(result.isFallbackUsed()).isTrue();
        assertThat(result.getSource()).isEqualTo("CIRCUIT_BREAKER_FALLBACK");
        assertThat(result.getItems()).isNotEmpty();
        assertThat(result.getSnippets()).isNotEmpty();

        // Kiểm tra item đầu tiên là Laptop QuickBook
        AiCatalogItemDto firstItem = result.getItems().get(0);
        assertThat(firstItem.getName()).contains("QuickBook");
        assertThat(firstItem.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("AiCatalogSearchService - Snippet sinh ra chứa đầy đủ thông tin hỗ trợ RAG Prompt")
    void testToSnippetFormat() {
        AiCatalogItemDto dto = AiCatalogItemDto.builder()
                .id(99L)
                .name("Tai nghe QuickPods Pro 2")
                .categoryName("Điện thoại & Phụ kiện")
                .minPrice(BigDecimal.valueOf(4990000))
                .maxPrice(BigDecimal.valueOf(4990000))
                .totalStock(80)
                .averageRating(4.9)
                .status("ACTIVE")
                .build();

        String snippet = dto.toSnippet();
        assertThat(snippet).contains("Tai nghe QuickPods Pro 2");
        assertThat(snippet).contains("Điện thoại & Phụ kiện");
        assertThat(snippet).contains("4.990.000");
        assertThat(snippet).contains("4.9★");
        assertThat(snippet).contains("80");
    }

    @Test
    @DisplayName("AiCatalogSearchService - Tra cứu trạng thái và reset Circuit Breaker")
    void testGetStatusAndReset() {
        Map<String, Object> status = catalogSearchService.getCircuitBreakerStatus();
        assertThat(status).containsEntry("state", "CLOSED");
        assertThat(status).containsEntry("failureThreshold", 3);

        catalogSearchService.resetCircuitBreaker();
        assertThat(catalogSearchService.getCircuitBreakerStatus()).containsEntry("state", "CLOSED");
    }
}
