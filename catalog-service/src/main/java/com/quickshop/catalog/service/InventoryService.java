package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.StockReserveItem;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.repository.ProductVariantRepository;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    public static final String INVENTORY_RESERVE_KEY_PREFIX = "inventory:reserve:";
    public static final Duration DEFAULT_RESERVE_TTL = Duration.ofMinutes(15); // 900 seconds (15 phút)

    private final ProductVariantRepository variantRepository;
    private final CatalogCacheService cacheService;
    private final StringRedisTemplate redisTemplate;

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

        // Lưu trạng thái giữ kho vào Redis với TTL 15 phút (900s)
        if (orderId != null && !orderId.isBlank()) {
            String redisKey = INVENTORY_RESERVE_KEY_PREFIX + orderId;
            try {
                redisTemplate.opsForValue().set(redisKey, "RESERVED", DEFAULT_RESERVE_TTL);
                log.info("Đã thiết lập Redis key giữ kho: {} với TTL {} giây", redisKey, DEFAULT_RESERVE_TTL.getSeconds());
            } catch (Exception ex) {
                log.warn("Lỗi khi lưu key giữ kho Redis {}: {}", redisKey, ex.getMessage());
            }
        }

        return true;
    }

    @Transactional
    public void releaseStock(String orderId, List<StockReserveItem> items) {
        log.info("Releasing reserved stock for orderId: {}, itemCount: {}", orderId, items != null ? items.size() : 0);
        if (items != null) {
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

        // Xóa key Redis giữ kho khi giải phóng tồn kho thành công
        if (orderId != null && !orderId.isBlank()) {
            String redisKey = INVENTORY_RESERVE_KEY_PREFIX + orderId;
            try {
                redisTemplate.delete(redisKey);
                log.info("Đã xóa Redis key giữ kho sau khi giải phóng: {}", redisKey);
            } catch (Exception ex) {
                log.warn("Lỗi khi xóa key giữ kho Redis {}: {}", redisKey, ex.getMessage());
            }
        }
    }

    @Transactional
    public void commitStock(String orderId, List<StockReserveItem> items) {
        log.info("Committing reserved stock after successful payment for orderId: {}", orderId);
        if (items != null) {
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

        // Xóa key Redis giữ kho khi commit tồn kho chính thức
        if (orderId != null && !orderId.isBlank()) {
            String redisKey = INVENTORY_RESERVE_KEY_PREFIX + orderId;
            try {
                redisTemplate.delete(redisKey);
                log.info("Đã xóa Redis key giữ kho sau khi commit: {}", redisKey);
            } catch (Exception ex) {
                log.warn("Lỗi khi xóa key giữ kho Redis {}: {}", redisKey, ex.getMessage());
            }
        }
    }

    public Long getReservationRemainingTtl(String orderId) {
        if (orderId == null || orderId.isBlank()) return -1L;
        String redisKey = INVENTORY_RESERVE_KEY_PREFIX + orderId;
        try {
            Long expire = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            return expire != null ? expire : -1L;
        } catch (Exception ex) {
            log.warn("Lỗi khi đọc TTL Redis key {}: {}", redisKey, ex.getMessage());
            return -1L;
        }
    }

    public boolean hasActiveReservation(String orderId) {
        if (orderId == null || orderId.isBlank()) return false;
        String redisKey = INVENTORY_RESERVE_KEY_PREFIX + orderId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception ex) {
            log.warn("Lỗi khi kiểm tra key Redis {}: {}", redisKey, ex.getMessage());
            return false;
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
