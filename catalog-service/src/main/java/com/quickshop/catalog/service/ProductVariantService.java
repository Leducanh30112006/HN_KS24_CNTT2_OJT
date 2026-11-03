package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.exception.ForbiddenException;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductVariantRepository;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final CatalogCacheService cacheService;

    public List<VariantDto> getVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        return variantRepository.findByProductId(productId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public VariantDto getVariantById(Long id) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));
        return mapToDto(variant);
    }

    public VariantDto getVariantBySku(String sku) {
        ProductVariant variant = variantRepository.findBySku(sku.trim())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "sku", sku));
        return mapToDto(variant);
    }

    @Transactional
    public VariantDto addVariant(Long productId, VariantCreateRequest request, Long authShopId, String callerRole) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        checkShopOwnership(product, authShopId, callerRole);

        // Kiểm tra giá bán không được âm
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá bán không được nhỏ hơn 0");
        }

        // Kiểm tra tồn kho không được âm
        if (request.getStock() != null && request.getStock() < 0) {
            throw new BadRequestException("Số lượng tồn kho không được nhỏ hơn 0");
        }

        String sku = request.getSku().trim();
        if (variantRepository.existsBySku(sku)) {
            throw new BadRequestException("Mã SKU '" + sku + "' đã tồn tại trên hệ thống");
        }

        ProductVariant variant = ProductVariant.builder()
                .productId(productId)
                .sku(sku)
                .color(request.getColor())
                .size(request.getSize())
                .price(request.getPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .reservedStock(0)
                .build();

        ProductVariant saved = variantRepository.save(variant);
        cacheService.evictProductCaches(productId);
        log.info("Thêm biến thể SKU {} cho sản phẩm ID {} thành công", sku, productId);

        return mapToDto(saved);
    }

    @Transactional
    public VariantDto updateVariant(Long id, VariantUpdateRequest request, Long authShopId, String callerRole) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));

        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", variant.getProductId()));

        checkShopOwnership(product, authShopId, callerRole);

        if (request.getPrice() != null) {
            if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Giá bán không được nhỏ hơn 0");
            }
            variant.setPrice(request.getPrice());
        }

        if (request.getStock() != null) {
            if (request.getStock() < 0) {
                throw new BadRequestException("Số lượng tồn kho không được nhỏ hơn 0");
            }
            variant.setStock(request.getStock());
        }

        if (request.getSku() != null && !request.getSku().isBlank()) {
            String newSku = request.getSku().trim();
            if (!newSku.equalsIgnoreCase(variant.getSku())) {
                Optional<ProductVariant> duplicate = variantRepository.findBySku(newSku);
                if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                    throw new BadRequestException("Mã SKU '" + newSku + "' đã được sử dụng bởi biến thể khác");
                }
                variant.setSku(newSku);
            }
        }

        if (request.getColor() != null) {
            variant.setColor(request.getColor());
        }
        if (request.getSize() != null) {
            variant.setSize(request.getSize());
        }

        ProductVariant updated = variantRepository.save(variant);
        cacheService.evictProductCaches(variant.getProductId());
        log.info("Cập nhật biến thể ID {} thành công", id);

        return mapToDto(updated);
    }

    @Transactional
    public VariantDto updatePrice(Long id, BigDecimal newPrice, Long authShopId, String callerRole) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá bán không được nhỏ hơn 0");
        }

        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));

        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", variant.getProductId()));

        checkShopOwnership(product, authShopId, callerRole);

        variant.setPrice(newPrice);
        ProductVariant updated = variantRepository.save(variant);
        cacheService.evictProductCaches(variant.getProductId());
        log.info("Cập nhật giá bán biến thể ID {} sang {} VND", id, newPrice);

        return mapToDto(updated);
    }

    @Transactional
    public VariantDto updateStock(Long id, VariantStockUpdateRequest request, Long authShopId, String callerRole) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));

        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", variant.getProductId()));

        checkShopOwnership(product, authShopId, callerRole);

        int targetStock;
        if (request.getNewStock() != null) {
            if (request.getNewStock() < 0) {
                throw new BadRequestException("Số lượng tồn kho không được nhỏ hơn 0");
            }
            targetStock = request.getNewStock();
        } else if (request.getQuantityAdjustment() != null) {
            targetStock = variant.getStock() + request.getQuantityAdjustment();
            if (targetStock < 0) {
                throw new BadRequestException("Số lượng tồn kho không được nhỏ hơn 0. Tồn kho hiện tại: "
                        + variant.getStock() + ", điều chỉnh: " + request.getQuantityAdjustment());
            }
        } else {
            throw new BadRequestException("Vui lòng cung cấp newStock hoặc quantityAdjustment");
        }

        variant.setStock(targetStock);
        ProductVariant updated = variantRepository.save(variant);
        cacheService.evictProductCaches(variant.getProductId());
        log.info("Điều chỉnh tồn kho biến thể ID {} thành {} (Lý do: {})", id, targetStock, request.getReason());

        return mapToDto(updated);
    }

    @Transactional
    public void deleteVariant(Long id, Long authShopId, String callerRole) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));

        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", variant.getProductId()));

        checkShopOwnership(product, authShopId, callerRole);

        List<ProductVariant> variants = variantRepository.findByProductId(variant.getProductId());
        if (variants.size() <= 1) {
            throw new BadRequestException("Không thể xóa biến thể duy nhất. Mỗi sản phẩm phải có ít nhất một biến thể SKU");
        }

        int reserved = variant.getReservedStock() != null ? variant.getReservedStock() : 0;
        if (reserved > 0) {
            throw new BadRequestException("Không thể xóa biến thể SKU " + variant.getSku() + " đang có " + reserved + " sản phẩm được giữ kho trong đơn hàng");
        }

        variantRepository.delete(variant);
        cacheService.evictProductCaches(variant.getProductId());
        log.info("Đã xóa biến thể ID {} của sản phẩm ID {}", id, variant.getProductId());
    }

    private void checkShopOwnership(Product product, Long authShopId, String callerRole) {
        if (isAdmin(callerRole)) {
            return;
        }
        if (authShopId != null && !authShopId.equals(product.getShopId())) {
            throw new ForbiddenException("Bạn không có quyền quản lý biến thể của gian hàng khác (Shop ID: " + product.getShopId() + ")");
        }
    }

    private boolean isAdmin(String role) {
        if (role == null) return false;
        return "ROLE_ADMIN".equalsIgnoreCase(role.trim()) || "ADMIN".equalsIgnoreCase(role.trim());
    }

    public VariantDto mapToDto(ProductVariant v) {
        int reserved = v.getReservedStock() != null ? v.getReservedStock() : 0;
        int stock = v.getStock() != null ? v.getStock() : 0;
        int available = Math.max(0, stock - reserved);

        return VariantDto.builder()
                .id(v.getId())
                .productId(v.getProductId())
                .sku(v.getSku())
                .color(v.getColor())
                .size(v.getSize())
                .price(v.getPrice())
                .stock(stock)
                .reservedStock(reserved)
                .availableStock(available)
                .build();
    }
}

