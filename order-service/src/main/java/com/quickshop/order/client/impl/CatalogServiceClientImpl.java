package com.quickshop.order.client.impl;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.order.client.CatalogServiceClient;
import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;
import com.quickshop.order.exception.CartOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class CatalogServiceClientImpl implements CatalogServiceClient {

    private final RestClient restClient;
    private final String catalogServiceUrl;
    private final boolean mockFallbackEnabled;

    public CatalogServiceClientImpl(
            RestClient.Builder restClientBuilder,
            @Value("${catalog.service.url:http://localhost:8082}") String catalogServiceUrl,
            @Value("${catalog.service.mock-enabled:true}") boolean mockFallbackEnabled) {
        this.catalogServiceUrl = catalogServiceUrl;
        this.mockFallbackEnabled = mockFallbackEnabled;
        this.restClient = restClientBuilder
                .baseUrl(catalogServiceUrl)
                .build();
    }

    @Override
    public Optional<CatalogVariantValidationDto> getVariantDetails(Long variantId) {
        if (variantId == null || variantId <= 0) {
            return Optional.empty();
        }

        try {
            ApiResponse<CatalogVariantValidationDto> response = restClient.get()
                    .uri("/api/catalog/variants/{variantId}/validation", variantId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.warn("Catalog Service phản hồi mã lỗi 4xx cho variantId {}: {}", variantId, resp.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<CatalogVariantValidationDto>>() {});

            if (response != null && response.isSuccess() && response.getData() != null) {
                return Optional.of(response.getData());
            }
            return Optional.empty();

        } catch (HttpClientErrorException.NotFound notFoundEx) {
            log.info("Không tìm thấy biến thể {} trên Catalog Service", variantId);
            return Optional.empty();

        } catch (Exception ex) {
            log.warn("[CATALOG_CONTRACT_MISSING] Không thể kết nối hoặc Catalog Service chưa cung cấp API: "
                            + "GET {}/api/catalog/variants/{}/validation. Chi tiết: {}",
                    catalogServiceUrl, variantId, ex.getMessage());

            if (mockFallbackEnabled) {
                log.info("[FALLBACK] Sử dụng Mock Data cho variantId {} để phục vụ kiểm thử Cart độc lập", variantId);
                return Optional.of(createMockVariant(variantId));
            }

            throw new CartOperationException(
                    "Không thể kết nối đến Catalog Service để xác thực biến thể sản phẩm. "
                            + "Yêu cầu Catalog Service cung cấp API: GET /api/catalog/variants/" + variantId + "/validation",
                    ex
            );
        }
    }

    private CatalogVariantValidationDto createMockVariant(Long variantId) {
        // Mock fallback thông minh phân bổ shopId theo id để test gom nhóm đa shop
        long mockShopId = (variantId % 3 == 0) ? 3L : (variantId % 2 == 0) ? 2L : 1L;
        String mockShopName = "Gian Hàng QuickShop " + mockShopId;

        return CatalogVariantValidationDto.builder()
                .variantId(variantId)
                .productId(variantId * 10)
                .shopId(mockShopId)
                .shopName(mockShopName)
                .productName("Sản phẩm mẫu QuickShop " + variantId)
                .variantName("Phiên bản tiêu chuẩn (Variant " + variantId + ")")
                .sku("SKU-VAR-" + variantId)
                .imageUrl("https://picsum.photos/seed/" + variantId + "/300/300")
                .price(BigDecimal.valueOf(100000L + (variantId * 25000L)))
                .stock(100)
                .reservedStock(10)
                .availableStock(90) // 100 - 10 = 90
                .active(true)
                .build();
    }
}
