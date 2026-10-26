package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.StockReserveItem;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductVariantRepository variantRepository;
    private final CatalogCacheService cacheService;

    @Transactional
    public boolean reserveStock(String orderId, List<StockReserveItem> items) {
        log.info("Processing Stock Reservation for orderId: {}, itemCount: {}", orderId, items.size());

        // 1. Verify availability for all items first (Atomic Check)
        for (StockReserveItem item : items) {
            ProductVariant variant = findVariant(item);
            if (variant == null) {
                log.error("Stock reservation failed: Variant not found (variantId: {}, sku: {})", item.getVariantId(), item.getSku());
                throw new IllegalStateException("Không tìm thấy biến thể sản phẩm SKU: " + item.getSku());
            }

            int requestedQty = item.getQuantity() != null ? item.getQuantity() : 1;
            if (variant.getStock() < requestedQty) {
                log.error("Stock reservation failed: Insufficient stock for SKU: {}. Available: {}, Requested: {}",
                        variant.getSku(), variant.getStock(), requestedQty);
                throw new IllegalStateException("Sản phẩm SKU " + variant.getSku() + " chỉ còn " + variant.getStock() + " trong kho");
            }
        }

        // 2. Perform stock reservation
        for (StockReserveItem item : items) {
            ProductVariant variant = findVariant(item);
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;

            variant.setStock(variant.getStock() - qty);
            variant.setReservedStock((variant.getReservedStock() != null ? variant.getReservedStock() : 0) + qty);
            variantRepository.save(variant);

            cacheService.evictProductCaches(variant.getProductId());
            log.info("Reserved {} units for SKU: {}, new stock: {}, reservedStock: {}",
                    qty, variant.getSku(), variant.getStock(), variant.getReservedStock());
        }

        return true;
    }

    @Transactional
    public void releaseStock(String orderId, List<StockReserveItem> items) {
        log.info("Releasing reserved stock for orderId: {}, itemCount: {}", orderId, items.size());

        for (StockReserveItem item : items) {
            ProductVariant variant = findVariant(item);
            if (variant != null) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                variant.setStock(variant.getStock() + qty);
                int currentReserved = variant.getReservedStock() != null ? variant.getReservedStock() : 0;
                variant.setReservedStock(Math.max(0, currentReserved - qty));
                variantRepository.save(variant);

                cacheService.evictProductCaches(variant.getProductId());
                log.info("Released {} units for SKU: {}, restored stock: {}", qty, variant.getSku(), variant.getStock());
            }
        }
    }

    @Transactional
    public void commitStock(String orderId, List<StockReserveItem> items) {
        log.info("Committing reserved stock after successful payment for orderId: {}", orderId);

        for (StockReserveItem item : items) {
            ProductVariant variant = findVariant(item);
            if (variant != null) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                int currentReserved = variant.getReservedStock() != null ? variant.getReservedStock() : 0;
                variant.setReservedStock(Math.max(0, currentReserved - qty));
                variantRepository.save(variant);
            }
        }
    }

    private ProductVariant findVariant(StockReserveItem item) {
        if (item.getVariantId() != null) {
            Optional<ProductVariant> opt = variantRepository.findById(item.getVariantId());
            if (opt.isPresent()) return opt.get();
        }
        if (item.getSku() != null && !item.getSku().isBlank()) {
            Optional<ProductVariant> opt = variantRepository.findBySku(item.getSku());
            if (opt.isPresent()) return opt.get();
        }
        return null;
    }
}
