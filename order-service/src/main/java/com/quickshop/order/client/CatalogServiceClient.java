package com.quickshop.order.client;

import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;

import java.util.Optional;

public interface CatalogServiceClient {

    /**
     * Tra cứu thông tin biến thể, giá bán, shop sở hữu và tồn kho khả dụng từ Catalog Service.
     * Contract yêu cầu: GET /api/catalog/variants/{variantId}/validation
     *
     * @param variantId ID biến thể SKU
     * @return Optional chứa thông tin hợp lệ hoặc Optional.empty() nếu không tìm thấy
     */
    Optional<CatalogVariantValidationDto> getVariantDetails(Long variantId);
}
