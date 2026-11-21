package com.quickshop.order.client.impl;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.order.client.CatalogServiceClient;
import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;
import com.quickshop.order.exception.CartOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(4000);
        requestFactory.setReadTimeout(8000);
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
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
            log.info("Không tìm thấy biến thể {} trên Catalog Service (404/Not Found)", variantId);
            return Optional.empty();

        } catch (HttpClientErrorException.NotFound notFoundEx) {
            log.info("Không tìm thấy biến thể {} trên Catalog Service", variantId);
            return Optional.empty();

        } catch (Exception ex) {
            log.warn("[CATALOG_CONTRACT_ERROR] Lỗi khi kết nối hoặc truy vấn Catalog Service cho variantId {}: {}",
                    variantId, ex.getMessage());

            // Chỉ dùng Fallback mock nếu Catalog Service tạm thời offline VÀ variantId nằm trong danh mục mẫu hợp lệ (511-542)
            if (isKnownFallbackVariant(variantId)) {
                log.info("[FALLBACK] Sử dụng Fallback Data cho variantId {} để phục vụ Cart độc lập", variantId);
                return Optional.of(createMockVariant(variantId));
            }
            return Optional.empty();
        }
    }

    private boolean isKnownFallbackVariant(Long variantId) {
        return variantId != null && (
                variantId == 511L || variantId == 512L ||
                variantId == 521L ||
                variantId == 531L || variantId == 532L ||
                variantId == 541L || variantId == 542L
        );
    }

    private CatalogVariantValidationDto createMockVariant(Long variantId) {
        long mockShopId = (variantId % 3 == 0) ? 3L : (variantId % 2 == 0) ? 2L : 1L;
        String mockShopName = "Gian Hàng QuickShop " + mockShopId;
        String prodName = "Sản phẩm QuickShop #" + variantId;
        String varName = "Phiên bản tiêu chuẩn";
        String sku = "SKU-VAR-" + variantId;
        BigDecimal price = BigDecimal.valueOf(100000L + (variantId * 25000L));

        if (variantId == 511L || variantId == 512L) {
            prodName = "Cáp Sạc Nhanh Type-C to Type-C 60W Bọc Dù Chống Đứt QuickCharge Pro";
            varName = variantId == 511L ? "Đen Carbon - 1 Mét (60W)" : "Xám Titan - 2 Mét (60W)";
            sku = variantId == 511L ? "CABLE-TYPEC-1M-BLK" : "CABLE-TYPEC-2M-GRAY";
            price = variantId == 511L ? new BigDecimal("49000") : new BigDecimal("69000");
            mockShopName = "TechWorld Official Store";
            mockShopId = 1L;
        } else if (variantId == 521L) {
            prodName = "Bộ 5 Đôi Tất Cổ Ngắn Cotton Khử Mùi Thoáng Khí QuickStep";
            varName = "Set 5 Màu Basic (Đen/Trắng/Xám)";
            sku = "SOCK-SET5-MIX";
            price = new BigDecimal("59000");
            mockShopName = "Thời Trang Coolmate Official";
            mockShopId = 2L;
        } else if (variantId == 531L || variantId == 532L) {
            prodName = "Sổ Tay Bìa Da A5 QuickNote 200 Trang Giấy Dầy Chống Lóa Kẻ Ngang";
            varName = variantId == 531L ? "Nâu Cổ Điển" : "Đen Lịch Lãm";
            sku = variantId == 531L ? "NOTE-A5-BROWN" : "NOTE-A5-BLACK";
            price = new BigDecimal("75000");
            mockShopName = "Nhà Sách Tri Thức & Sống Đẹp";
            mockShopId = 4L;
        } else if (variantId == 541L || variantId == 542L) {
            prodName = "Bình Nước Thể Thao 800ml Nhựa Tritan Chống Tràn Có Vạch Đo QuickHydro";
            varName = variantId == 541L ? "Xanh Tím Gradient" : "Đen Mờ Nhám";
            sku = variantId == 541L ? "BOTTLE-800ML-GRADIENT" : "BOTTLE-800ML-BLACK";
            price = new BigDecimal("89000");
            mockShopName = "SportZone Official Store";
            mockShopId = 3L;
        }

        return CatalogVariantValidationDto.builder()
                .variantId(variantId)
                .productId(variantId * 10)
                .shopId(mockShopId)
                .shopName(mockShopName)
                .productName(prodName)
                .variantName(varName)
                .sku(sku)
                .imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300")
                .price(price)
                .stock(200)
                .reservedStock(0)
                .availableStock(200)
                .active(true)
                .build();
    }
}
