package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.StockReserveItem;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.repository.ProductVariantRepository;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
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
        log.info("Processing Stock Reservation for orderId: {}, itemCount: {}", orderId, items != null ? items.size() : 0);

        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Danh sách sản phẩm giữ kho không được để trống");
        }

        // 1. Verify availability for all items first (Atomic Check)
        for (StockReserveItem item : items) {
            ProductVariant variant = findVariant(item);
            if (variant == null) {
                log.error("Stock reservation failed: Variant not found (variantId: {}, sku: {})", item.getVariantId(), item.getSku());
                throw new IllegalStateException("Không tìm thấy biến thể sản phẩm SKU: " + item.getSku());
            }

            int requestedQty = item.getQuantity() != null ? item.getQuantity() : 1;
            if (requestedQty <= 0) {
                throw new BadRequestException("Số lượng đặt mua phải lớn hơn 0");
            }

            int currentStock = variant.getStock() != null ? variant.getStock() : 0;
            if (currentStock < requestedQty) {
                log.error("Stock reservation failed: Insufficient stock for SKU: {}. Available: {}, Requested: {}",
                        variant.getSku(), currentStock, requestedQty);
                throw new IllegalStateException("Sản phẩm SKU " + variant.getSku() + " chỉ còn " + currentStock + " trong kho (yêu cầu: " + requestedQty + ")");
            }
        }

        // 2. Perform stock reservation - Không cho tồn kho âm
        for (StockReserveItem item : items) {
            ProductVariant variant = findVariant(item);
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;

            int currentStock = variant.getStock() != null ? variant.getStock() : 0;
            int newStock = currentStock - qty;
            if (newStock < 0) {
                throw new IllegalStateException("Tồn kho không được phép âm cho biến thể SKU: " + variant.getSku());
            }

            variant.setStock(newStock);
            int currentReserved = variant.getReservedStock() != null ? variant.getReservedStock() : 0;
            variant.setReservedStock(currentReserved + qty);
            variantRepository.save(variant);

            cacheService.evictProductCaches(variant.getProductId());
            log.info("Reserved {} units for SKU: {}, new stock: {}, reservedStock: {}",
                    qty, variant.getSku(), variant.getStock(), variant.getReservedStock());
        }

        return true;
    }

    @Transactional
    public void releaseStock(String orderId, List<StockReserveItem> items) {
        log.info("Releasing reserved stock for orderId: {}, itemCount: {}", orderId, items != null ? items.size() : 0);
        if (items == null) return;

        for (StockReserveItem item : items) {
            ProductVariant variant = findVariant(item);
            if (variant != null) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                int currentStock = variant.getStock() != null ? variant.getStock() : 0;
                variant.setStock(Math.max(0, currentStock + qty));

                int currentReserved = variant.getReservedStock() != null ? variant.getReservedStock() : 0;
                variant.setReservedStock(Math.max(0, currentReserved - qty));
                variantRepository.save(variant);

                cacheService.evictProductCaches(variant.getProductId());
                log.info("Released {} units for SKU: {}, restored stock: {}, remaining reservedStock: {}",
                        qty, variant.getSku(), variant.getStock(), variant.getReservedStock());
            }
        }
    }

    @Transactional
    public void commitStock(String orderId, List<StockReserveItem> items) {
        log.info("Committing reserved stock after successful payment for orderId: {}", orderId);
        if (items == null) return;

        for (StockReserveItem item : items) {
            ProductVariant variant = findVariant(item);
            if (variant != null) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                int currentReserved = variant.getReservedStock() != null ? variant.getReservedStock() : 0;
                variant.setReservedStock(Math.max(0, currentReserved - qty));
                variantRepository.save(variant);
                log.info("Committed {} units for SKU: {}, remaining reservedStock: {}",
                        qty, variant.getSku(), variant.getReservedStock());
            }
        }
    }

    @Transactional
    public ProductVariant adjustStock(Long variantId, int adjustment) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        int currentStock = variant.getStock() != null ? variant.getStock() : 0;
        int newStock = currentStock + adjustment;
        if (newStock < 0) {
            throw new BadRequestException("Số lượng tồn kho không được nhỏ hơn 0. Tồn kho hiện tại: "
                    + currentStock + ", điều chỉnh: " + adjustment);
        }

        variant.setStock(newStock);
        ProductVariant saved = variantRepository.save(variant);
        cacheService.evictProductCaches(variant.getProductId());
        log.info("Điều chỉnh tồn kho variant ID {}: {} -> {}", variantId, currentStock, newStock);
        return saved;
    }

    public boolean isStockAvailable(Long variantId, int requestedQty) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        int currentStock = variant.getStock() != null ? variant.getStock() : 0;
        return currentStock >= requestedQty;
    }

    private ProductVariant findVariant(StockReserveItem item) {
        if (item.getVariantId() != null) {
            Optional<ProductVariant> opt = variantRepository.findById(item.getVariantId());
            if (opt.isPresent()) return opt.get();
        }
        if (item.getSku() != null && !item.getSku().isBlank()) {
            Optional<ProductVariant> opt = variantRepository.findBySku(item.getSku().trim());
            if (opt.isPresent()) return opt.get();
        }
        return null;
    }
}
