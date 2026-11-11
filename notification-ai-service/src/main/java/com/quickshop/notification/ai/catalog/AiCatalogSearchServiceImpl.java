package com.quickshop.notification.ai.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ tìm kiếm Catalog cho AI với cơ chế chịu lỗi cao
 * - Timeout kết nối 2s, đọc 3s
 * - Circuit Breaker tự động ngắt mạch khi lỗi 3 lần liên tiếp
 * - Fallback sang bộ dữ liệu sản phẩm mẫu đã kiểm duyệt
 * - Kiểm soát quyền truy cập chỉ cho phép hiển thị sản phẩm ACTIVE/APPROVED
 */
@Slf4j
@Service
public class AiCatalogSearchServiceImpl implements AiCatalogSearchService {

    private final RestClient restClient;
    private final String catalogServiceUrl;
    private final AiCatalogCircuitBreaker circuitBreaker;
    private final AiCatalogAccessValidator accessValidator;
    private final ObjectMapper objectMapper;

    // Bộ dữ liệu sản phẩm dự phòng an toàn (Curated Safe Fallback Knowledge Base)
    private static final List<AiCatalogItemDto> CURATED_FALLBACK_CATALOG = List.of(
            AiCatalogItemDto.builder()
                    .id(1L)
                    .name("Điện thoại thông minh QuickPhone Pro Max 256GB - Bản Quốc Tế")
                    .categoryId(4L)
                    .categoryName("Điện thoại & Phụ kiện")
                    .shopName("Gian Hàng Chính Hãng QuickShop")
                    .minPrice(new BigDecimal("29990000"))
                    .maxPrice(new BigDecimal("41990000"))
                    .totalStock(85)
                    .averageRating(5.0)
                    .reviewCount(2L)
                    .status("ACTIVE")
                    .description("Flagship chip AI 3nm mạnh mẽ, màn hình OLED 120Hz, camera zoom quang học 5x, pin trâu 29h.")
                    .build(),
            AiCatalogItemDto.builder()
                    .id(2L)
                    .name("Laptop QuickBook Ultra 14 inch M3 Pro - 18GB/512GB")
                    .categoryId(5L)
                    .categoryName("Laptop & Máy tính")
                    .shopName("Gian Hàng Chính Hãng QuickShop")
                    .minPrice(new BigDecimal("45990000"))
                    .maxPrice(new BigDecimal("57990000"))
                    .totalStock(30)
                    .averageRating(5.0)
                    .reviewCount(1L)
                    .status("ACTIVE")
                    .description("Màn hình Liquid Retina XDR, chip 11-core CPU, 14-core GPU xử lý đồ họa và build Docker/IDE siêu mượt.")
                    .build(),
            AiCatalogItemDto.builder()
                    .id(3L)
                    .name("Tai nghe Bluetooth True Wireless QuickPods Pro 2 Chống Ồn Chủ Động")
                    .categoryId(4L)
                    .categoryName("Điện thoại & Phụ kiện")
                    .shopName("Gian Hàng Chính Hãng QuickShop")
                    .minPrice(new BigDecimal("4990000"))
                    .maxPrice(new BigDecimal("4990000"))
                    .totalStock(80)
                    .averageRating(5.0)
                    .reviewCount(1L)
                    .status("ACTIVE")
                    .description("Âm thanh vòm chuẩn phòng thu, khử tiếng ồn chủ động ANC thế hệ 2, kháng nước IP54.")
                    .build(),
            AiCatalogItemDto.builder()
                    .id(4L)
                    .name("Áo Polo Nam QuickStyle Premium Vải Cafe Khử Mùi Thoáng Khí")
                    .categoryId(6L)
                    .categoryName("Thời trang nam nữ")
                    .shopName("Gian Hàng Thời Trang QuickStyle")
                    .minPrice(new BigDecimal("289000"))
                    .maxPrice(new BigDecimal("289000"))
                    .totalStock(255)
                    .averageRating(5.0)
                    .reviewCount(1L)
                    .status("ACTIVE")
                    .description("Chất liệu dệt sợi bã cafe tự nhiên khử mùi, co giãn 4 chiều, chống tia UV 50+.")
                    .build(),
            AiCatalogItemDto.builder()
                    .id(7L)
                    .name("Giày Sneaker Thể Thao QuickRunner Pro Đệm Khí Êm Ái Chống Trượt")
                    .categoryId(7L)
                    .categoryName("Giày dép & Túi xách")
                    .shopName("Gian Hàng Thể Thao QuickRunner")
                    .minPrice(new BigDecimal("890000"))
                    .maxPrice(new BigDecimal("890000"))
                    .totalStock(120)
                    .averageRating(5.0)
                    .reviewCount(0L)
                    .status("ACTIVE")
                    .description("Đế đệm khí êm nhẹ trợ lực khi chạy bộ, dệt lưới Flyknit thoáng khí kháng khuẩn.")
                    .build(),
            AiCatalogItemDto.builder()
                    .id(6L)
                    .name("Tinh Chất Serum Niacinamide 10% + Zinc 1% Dưỡng Sáng Đều Màu Da")
                    .categoryId(8L)
                    .categoryName("Mỹ phẩm & Làm đẹp")
                    .shopName("Gian Hàng Mỹ Phẩm Chính Hãng")
                    .minPrice(new BigDecimal("199000"))
                    .maxPrice(new BigDecimal("349000"))
                    .totalStock(240)
                    .averageRating(5.0)
                    .reviewCount(0L)
                    .status("ACTIVE")
                    .description("Công thức lành tính dịu nhẹ kiểm soát bã nhờn, làm thông thoáng lỗ chân lông, mờ thâm mụn sau 14 ngày.")
                    .build(),
            AiCatalogItemDto.builder()
                    .id(5L)
                    .name("Robot Hút Bụi Lau Nhà Tự Động QuickBot Omni 3000Pa Cảm Biến LiDAR")
                    .categoryId(9L)
                    .categoryName("Nhà cửa & Đời sống")
                    .shopName("Gian Hàng Gia Dụng Thông Minh")
                    .minPrice(new BigDecimal("8490000"))
                    .maxPrice(new BigDecimal("8490000"))
                    .totalStock(30)
                    .averageRating(5.0)
                    .reviewCount(0L)
                    .status("ACTIVE")
                    .description("Hệ thống laser LDS 360 độ lập bản đồ 3D cực chuẩn, lực hút 3000Pa gom sạch lông thú cưng, tự giặt sấy giẻ lau.")
                    .build()
    );

    public AiCatalogSearchServiceImpl(
            @Value("${catalog.service.url:http://localhost:8082}") String catalogServiceUrl,
            @Value("${catalog.service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${catalog.service.read-timeout-ms:3000}") int readTimeoutMs,
            AiCatalogCircuitBreaker circuitBreaker,
            AiCatalogAccessValidator accessValidator,
            ObjectMapper objectMapper) {
        this.catalogServiceUrl = catalogServiceUrl;
        this.circuitBreaker = circuitBreaker;
        this.accessValidator = accessValidator;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(catalogServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AiCatalogSearchResult searchProducts(AiCatalogSearchRequest request) {
        long startTime = System.currentTimeMillis();

        if (request == null) {
            request = new AiCatalogSearchRequest();
        }

        // 1. Làm sạch từ khóa tìm kiếm (Data Sanitization & Injection Prevention)
        String sanitizedKeyword = accessValidator.sanitizeSearchKeyword(request.getKeyword());
        int limit = (request.getLimit() > 0 && request.getLimit() <= 10) ? request.getLimit() : 5;

        // 2. Kiểm tra Circuit Breaker trước khi gửi lệnh gọi mạng
        if (!circuitBreaker.allowRequest()) {
            log.warn("[AI_CATALOG_SEARCH] Circuit Breaker đang OPEN! Ngắt kết nối mạng và kích hoạt Fallback Cache");
            List<AiCatalogItemDto> fallbackItems = getFallbackItems(sanitizedKeyword);
            List<AiCatalogItemDto> filteredFallback = accessValidator.filterAccessibleProducts(fallbackItems, request.getUserRole(), limit);
            List<String> snippets = filteredFallback.stream().map(AiCatalogItemDto::toSnippet).collect(Collectors.toList());

            return AiCatalogSearchResult.builder()
                    .items(filteredFallback)
                    .snippets(snippets)
                    .source("CIRCUIT_BREAKER_FALLBACK")
                    .fallbackUsed(true)
                    .circuitBreakerState(CircuitBreakerState.OPEN)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 3. Thực hiện gọi Catalog Service API
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(catalogServiceUrl)
                    .path("/api/catalog/products")
                    .queryParam("status", "ACTIVE")
                    .queryParam("page", 0)
                    .queryParam("size", limit);

            if (!sanitizedKeyword.isBlank()) {
                uriBuilder.queryParam("keyword", sanitizedKeyword);
            }
            if (request.getCategoryId() != null && request.getCategoryId() > 0) {
                uriBuilder.queryParam("categoryId", request.getCategoryId());
            }
            if (request.getMinPrice() != null) {
                uriBuilder.queryParam("minPrice", request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                uriBuilder.queryParam("maxPrice", request.getMaxPrice());
            }

            URI targetUri = uriBuilder.build().encode().toUri();

            String responseBody = restClient.get()
                    .uri(targetUri)
                    .retrieve()
                    .body(String.class);

            List<AiCatalogItemDto> parsedItems = parseCatalogProducts(responseBody);

            // 4. Kiểm tra quyền truy cập và lọc bảo vệ dữ liệu sản phẩm
            List<AiCatalogItemDto> safeItems = accessValidator.filterAccessibleProducts(parsedItems, request.getUserRole(), limit);

            // Nếu không tìm thấy sản phẩm nào từ từ khóa, bổ sung fallback gợi ý phù hợp
            boolean fallbackUsed = false;
            String source = "CATALOG_SERVICE";
            if (safeItems.isEmpty()) {
                log.info("[AI_CATALOG_SEARCH] Catalog API không có sản phẩm khớp từ khóa '{}', chuyển sang Fallback Knowledge", sanitizedKeyword);
                safeItems = accessValidator.filterAccessibleProducts(getFallbackItems(sanitizedKeyword), request.getUserRole(), limit);
                fallbackUsed = true;
                source = "FALLBACK_CACHE";
            } else {
                circuitBreaker.recordSuccess();
            }

            List<String> snippets = safeItems.stream().map(AiCatalogItemDto::toSnippet).collect(Collectors.toList());

            return AiCatalogSearchResult.builder()
                    .items(safeItems)
                    .snippets(snippets)
                    .source(source)
                    .fallbackUsed(fallbackUsed)
                    .circuitBreakerState(circuitBreaker.getState())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            circuitBreaker.recordFailure(e.getMessage());
            log.warn("[AI_CATALOG_SEARCH] Gọi Catalog API thất bại ({}). Kích hoạt Fallback an toàn cho AI.", e.getMessage());

            List<AiCatalogItemDto> fallbackItems = getFallbackItems(sanitizedKeyword);
            List<AiCatalogItemDto> filtered = accessValidator.filterAccessibleProducts(fallbackItems, request.getUserRole(), limit);
            List<String> snippets = filtered.stream().map(AiCatalogItemDto::toSnippet).collect(Collectors.toList());

            return AiCatalogSearchResult.builder()
                    .items(filtered)
                    .snippets(snippets)
                    .source("CIRCUIT_BREAKER_FALLBACK")
                    .fallbackUsed(true)
                    .circuitBreakerState(circuitBreaker.getState())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public List<AiCatalogItemDto> getFallbackItems(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>(CURATED_FALLBACK_CATALOG);
        }

        String lower = keyword.toLowerCase();
        List<AiCatalogItemDto> matched = CURATED_FALLBACK_CATALOG.stream()
                .filter(p -> p.getName().toLowerCase().contains(lower) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower)) ||
                        (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(lower)))
                .collect(Collectors.toList());

        // Nếu từ khóa không khớp chính xác, trả về toàn bộ danh mục mẫu đã kiểm duyệt
        if (matched.isEmpty()) {
            return new ArrayList<>(CURATED_FALLBACK_CATALOG);
        }
        return matched;
    }

    @Override
    public AiCatalogCircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    @Override
    public Map<String, Object> getCircuitBreakerStatus() {
        return circuitBreaker.getStatus();
    }

    @Override
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
    }

    private List<AiCatalogItemDto> parseCatalogProducts(String responseBody) {
        List<AiCatalogItemDto> items = new ArrayList<>();
        if (responseBody == null || responseBody.isBlank()) {
            return items;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.path("data");
            JsonNode contentArray = dataNode.path("content");

            if (contentArray.isArray()) {
                for (JsonNode node : contentArray) {
                    AiCatalogItemDto item = objectMapper.treeToValue(node, AiCatalogItemDto.class);
                    if (item != null) {
                        items.add(item);
                    }
                }
            } else if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    AiCatalogItemDto item = objectMapper.treeToValue(node, AiCatalogItemDto.class);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[AI_CATALOG_SEARCH] Không thể parse JSON kết quả Catalog API: {}", e.getMessage());
        }
        return items;
    }
}
