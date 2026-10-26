package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.entity.Category;
import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.repository.CategoryRepository;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductReviewRepository;
import com.quickshop.catalog.repository.ProductVariantRepository;
import com.quickshop.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductReviewRepository reviewRepository;
    private final CatalogCacheService cacheService;

    public PageResult<ProductDto> getProducts(
            String keyword,
            Long categoryId,
            Long shopId,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            int page,
            int size,
            String sortBy
    ) {
        String filterHash = String.format("kw_%s_cat_%s_shop_%s_st_%s_p_%d_s_%d_sort_%s_minP_%s_maxP_%s_rate_%s",
                keyword != null ? keyword.trim() : "all",
                categoryId != null ? categoryId : "all",
                shopId != null ? shopId : "all",
                status != null ? status.name() : "ACTIVE",
                page, size, sortBy != null ? sortBy : "newest",
                minPrice != null ? minPrice.toPlainString() : "0",
                maxPrice != null ? maxPrice.toPlainString() : "max",
                minRating != null ? minRating : "0"
        );

        // 1. Try Redis cache (TTL 10m - FR-03)
        PageResult<ProductDto> cachedPage = cacheService.getCachedProductPage(filterHash);
        if (cachedPage != null) {
            return cachedPage;
        }

        // 2. Build Spring Data JPA Specification
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter (Default ACTIVE for storefront buyers)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            // Shop filter
            if (shopId != null) {
                predicates.add(cb.equal(root.get("shopId"), shopId));
            }

            // Keyword search in name and description
            if (keyword != null && !keyword.isBlank()) {
                String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(nameLike, descLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 3. Sorting
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("newest".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("oldest".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("nameAsc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // 4. Map to DTOs and calculate prices/stock/ratings
        List<ProductDto> dtos = productPage.getContent().stream()
                .map(this::mapToDto)
                .filter(dto -> {
                    // Optional price filtering
                    if (minPrice != null && dto.getMinPrice() != null && dto.getMinPrice().compareTo(minPrice) < 0) {
                        return false;
                    }
                    if (maxPrice != null && dto.getMaxPrice() != null && dto.getMaxPrice().compareTo(maxPrice) > 0) {
                        return false;
                    }
                    // Optional rating filtering
                    if (minRating != null && dto.getAverageRating() != null && dto.getAverageRating() < minRating) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        PageResult<ProductDto> result = PageResult.<ProductDto>builder()
                .content(dtos)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();

        // 5. Cache result to Redis
        cacheService.cacheProductPage(filterHash, result);
        return result;
    }

    public ProductDto getProductById(Long id) {
        // 1. Try Redis cache (TTL 10m)
        ProductDto cached = cacheService.getCachedProduct(id);
        if (cached != null) {
            return cached;
        }

        // 2. Fetch from DB
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        ProductDto dto = mapToDto(product);

        // 3. Save to Redis
        cacheService.cacheProduct(id, dto);
        return dto;
    }

    public PageResult<ProductDto> getProductsByShop(Long shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findByShopId(shopId, pageable);

        List<ProductDto> dtos = productPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PageResult.<ProductDto>builder()
                .content(dtos)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request, Long authShopId) {
        Long finalShopId = authShopId != null ? authShopId : (request.getShopId() != null ? request.getShopId() : 1L);

        Product product = Product.builder()
                .shopId(finalShopId)
                .categoryId(request.getCategoryId())
                .name(request.getName().trim())
                .description(request.getDescription())
                .mainImage(request.getMainImage())
                .status(ProductStatus.PENDING_APPROVAL) // FR-04: Tạo ở trạng thái PENDING_APPROVAL
                .build();

        Product savedProduct = productRepository.save(product);

        // Save initial variants
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (VariantCreateRequest vReq : request.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .productId(savedProduct.getId())
                        .sku(vReq.getSku().trim())
                        .color(vReq.getColor())
                        .size(vReq.getSize())
                        .price(vReq.getPrice())
                        .stock(vReq.getStock() != null ? vReq.getStock() : 0)
                        .reservedStock(0)
                        .build();
                variantRepository.save(variant);
            }
        }

        // Invalidate Redis caches
        cacheService.evictProductCaches(savedProduct.getId());
        log.info("Created new product ID {} with status PENDING_APPROVAL for shopId {}", savedProduct.getId(), finalShopId);

        return mapToDto(savedProduct);
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductUpdateRequest request, Long shopId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getMainImage() != null && !request.getMainImage().isBlank()) {
            product.setMainImage(request.getMainImage());
        }
        if (request.getCategoryId() != null) {
            product.setCategoryId(request.getCategoryId());
        }

        Product saved = productRepository.save(product);
        cacheService.evictProductCaches(id);

        return mapToDto(saved);
    }

    @Transactional
    public ProductDto updateProductStatus(Long id, ProductStatus newStatus, String adminNote) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setStatus(newStatus);
        Product saved = productRepository.save(product);

        cacheService.evictProductCaches(id);
        log.info("Admin updated status for product id {} to {}", id, newStatus);

        return mapToDto(saved);
    }

    @Transactional
    public void deleteProduct(Long id, Long shopId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        variantRepository.deleteByProductId(id);
        productRepository.delete(product);
        cacheService.evictProductCaches(id);

        log.info("Deleted product id {}", id);
    }

    public ProductDto mapToDto(Product product) {
        List<ProductVariant> variants = variantRepository.findByProductId(product.getId());

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        int totalStock = 0;

        List<VariantDto> variantDtos = new ArrayList<>();
        for (ProductVariant v : variants) {
            if (minPrice == null || v.getPrice().compareTo(minPrice) < 0) {
                minPrice = v.getPrice();
            }
            if (maxPrice == null || v.getPrice().compareTo(maxPrice) > 0) {
                maxPrice = v.getPrice();
            }
            totalStock += (v.getStock() != null ? v.getStock() : 0);

            int reserved = v.getReservedStock() != null ? v.getReservedStock() : 0;
            int available = Math.max(0, (v.getStock() != null ? v.getStock() : 0) - reserved);

            variantDtos.add(VariantDto.builder()
                    .id(v.getId())
                    .productId(v.getProductId())
                    .sku(v.getSku())
                    .color(v.getColor())
                    .size(v.getSize())
                    .price(v.getPrice())
                    .stock(v.getStock())
                    .reservedStock(reserved)
                    .availableStock(available)
                    .build());
        }

        // Category info
        String categoryName = "Danh mục chung";
        if (product.getCategoryId() != null) {
            categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName)
                    .orElse("Danh mục chung");
        }

        // Ratings & Reviews count
        long reviewCount = reviewRepository.countByProductId(product.getId());
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());
        if (avgRating == null) {
            avgRating = 5.0; // Default 5.0 star for new products
        } else {
            avgRating = Math.round(avgRating * 10.0) / 10.0;
        }

        return ProductDto.builder()
                .id(product.getId())
                .shopId(product.getShopId())
                .shopName("TechWorld Official Store") // In real flow, join with Shop or fetch
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .name(product.getName())
                .description(product.getDescription())
                .mainImage(product.getMainImage())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .minPrice(minPrice != null ? minPrice : BigDecimal.ZERO)
                .maxPrice(maxPrice != null ? maxPrice : BigDecimal.ZERO)
                .totalStock(totalStock)
                .averageRating(avgRating)
                .reviewCount(reviewCount)
                .variants(variantDtos)
                .build();
    }
}
