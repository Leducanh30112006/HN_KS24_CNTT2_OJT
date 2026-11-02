package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.entity.Category;
import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.exception.ForbiddenException;
import com.quickshop.catalog.repository.CategoryRepository;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductReviewRepository;
import com.quickshop.catalog.repository.ProductVariantRepository;
import com.quickshop.common.exception.BadRequestException;
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
                status != null ? status.name() : "PUBLIC_APPROVED",
                page, size, sortBy != null ? sortBy : "newest",
                minPrice != null ? minPrice.toPlainString() : "0",
                maxPrice != null ? maxPrice.toPlainString() : "max",
                minRating != null ? minRating : "0"
        );

        // 1. Kiểm tra Redis cache (TTL 10 phút)
        PageResult<ProductDto> cachedPage = cacheService.getCachedProductPage(filterHash);
        if (cachedPage != null) {
            return cachedPage;
        }

        // 2. Tạo Spring Data JPA Specification
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Bộ lọc trạng thái: nếu không truyền status, mặc định chỉ hiển thị sản phẩm đã APPROVED hoặc ACTIVE cho khách
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(root.get("status").in(List.of(ProductStatus.APPROVED, ProductStatus.ACTIVE)));
            }

            // Bộ lọc danh mục (Hỗ trợ cả danh mục lá và cây danh mục cha-con)
            if (categoryId != null) {
                List<Category> childCategories = categoryRepository.findByParentId(categoryId);
                if (childCategories != null && !childCategories.isEmpty()) {
                    List<Long> categoryIds = new ArrayList<>();
                    categoryIds.add(categoryId);
                    for (Category child : childCategories) {
                        categoryIds.add(child.getId());
                    }
                    predicates.add(root.get("categoryId").in(categoryIds));
                } else {
                    predicates.add(cb.equal(root.get("categoryId"), categoryId));
                }
            }

            // Bộ lọc gian hàng (Shop ID)
            if (shopId != null) {
                predicates.add(cb.equal(root.get("shopId"), shopId));
            }

            // Tìm kiếm theo từ khóa trong tên và mô tả
            if (keyword != null && !keyword.isBlank()) {
                String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 3. Phân trang & sắp xếp
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "id");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "id");
        } else if ("popular".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "id");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // 4. Ánh xạ sang DTO kèm thông tin biến thể & rating
        List<ProductDto> dtoList = productPage.getContent().stream()
                .map(this::mapToDto)
                .filter(dto -> {
                    if (minPrice != null && dto.getMinPrice().compareTo(minPrice) < 0) {
                        return false;
                    }
                    if (maxPrice != null && dto.getMaxPrice().compareTo(maxPrice) > 0) {
                        return false;
                    }
                    if (minRating != null && dto.getAverageRating() < minRating) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        PageResult<ProductDto> result = PageResult.<ProductDto>builder()
                .content(dtoList)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();

        // 5. Lưu kết quả vào Redis Cache
        cacheService.cacheProductPage(filterHash, result);

        return result;
    }

    public ProductDto getProductById(Long id, Long authShopId, String callerRole) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Kiểm tra quyền truy cập: nếu sản phẩm chưa APPROVED/ACTIVE thì chỉ Admin hoặc chính Shop sở hữu mới được xem
        if (!product.getStatus().isApproved()) {
            boolean isAdmin = isAdmin(callerRole);
            boolean isOwner = authShopId != null && authShopId.equals(product.getShopId());
            if (!isAdmin && !isOwner) {
                log.warn("Truy cập trái phép: shopId {} hoặc role {} không được xem sản phẩm {} ở trạng thái {}",
                        authShopId, callerRole, id, product.getStatus());
                throw new ForbiddenException("Bạn không có quyền truy cập sản phẩm chưa được công khai (Trạng thái: " + product.getStatus() + ")");
            }
        }

        // Cache cho sản phẩm public
        if (product.getStatus().isApproved()) {
            ProductDto cached = cacheService.getCachedProduct(id);
            if (cached != null) {
                return cached;
            }
        }

        ProductDto dto = mapToDto(product);

        if (product.getStatus().isApproved()) {
            cacheService.cacheProduct(id, dto);
        }

        return dto;
    }

    public PageResult<ProductDto> getProductsByShop(Long shopId, ProductStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage;

        if (status != null) {
            productPage = productRepository.findByShopIdAndStatus(shopId, status, pageable);
        } else {
            productPage = productRepository.findByShopId(shopId, pageable);
        }

        List<ProductDto> dtoList = productPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PageResult.<ProductDto>builder()
                .content(dtoList)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request, Long authShopId, String callerRole) {
        // Kiểm tra phân quyền: Không cho phép BUYER tạo sản phẩm
        if (isBuyer(callerRole)) {
            throw new ForbiddenException("Chỉ người bán (ROLE_SELLER) hoặc quản trị viên (ROLE_ADMIN) mới có quyền tạo sản phẩm");
        }

        Long finalShopId = authShopId != null ? authShopId : (request.getShopId() != null ? request.getShopId() : 1L);

        // Xác định trạng thái ban đầu: DRAFT hoặc PENDING
        ProductStatus initialStatus = ProductStatus.PENDING;
        if (Boolean.TRUE.equals(request.getIsDraft()) || request.getStatus() == ProductStatus.DRAFT) {
            initialStatus = ProductStatus.DRAFT;
        } else if (request.getStatus() != null) {
            // Nếu người bán yêu cầu APPROVED mà không phải Admin -> ép về PENDING
            if (request.getStatus().isApproved() && !isAdmin(callerRole)) {
                initialStatus = ProductStatus.PENDING;
            } else {
                initialStatus = request.getStatus();
            }
        }

        Product product = Product.builder()
                .shopId(finalShopId)
                .categoryId(request.getCategoryId())
                .name(request.getName().trim())
                .description(request.getDescription())
                .mainImage(request.getMainImage())
                .status(initialStatus)
                .build();

        Product savedProduct = productRepository.save(product);

        // Lưu các biến thể SKU
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

        cacheService.evictProductCaches(savedProduct.getId());
        log.info("Đã tạo sản phẩm mới ID {} ở trạng thái {} cho shopId {}", savedProduct.getId(), initialStatus, finalShopId);

        return mapToDto(savedProduct);
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductUpdateRequest request, Long authShopId, String callerRole) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Kiểm tra quyền sở hữu Shop: nếu không phải Admin thì phải chính là chủ gian hàng
        if (!isAdmin(callerRole)) {
            if (authShopId != null && !authShopId.equals(product.getShopId())) {
                log.warn("Từ chối quyền cập nhật: caller shopId {} cố sửa sản phẩm của shopId {}", authShopId, product.getShopId());
                throw new ForbiddenException("Bạn không có quyền chỉnh sửa sản phẩm thuộc gian hàng khác (Shop ID: " + product.getShopId() + ")");
            }
        }

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

        // Nếu sản phẩm từng bị REJECTED và Seller sửa lại, hỗ trợ chuyển về DRAFT hoặc PENDING
        if (product.getStatus() == ProductStatus.REJECTED) {
            if (request.getStatus() != null) {
                product.setStatus(request.getStatus());
            } else {
                product.setStatus(ProductStatus.DRAFT);
            }
            product.setRejectionReason(null);
        } else if (request.getStatus() != null) {
            // Cho phép seller tự chuyển giữa DRAFT và PENDING
            if (request.getStatus() == ProductStatus.DRAFT || request.getStatus() == ProductStatus.PENDING) {
                product.setStatus(request.getStatus());
            } else if (isAdmin(callerRole)) {
                product.setStatus(request.getStatus());
            }
        }

        // Cập nhật lại danh sách biến thể nếu có
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            variantRepository.deleteByProductId(id);
            for (VariantCreateRequest vReq : request.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .productId(id)
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

        Product saved = productRepository.save(product);
        cacheService.evictProductCaches(id);

        log.info("Cập nhật thành công sản phẩm ID {} bởi shopId {}", id, authShopId);
        return mapToDto(saved);
    }

    @Transactional
    public ProductDto submitForApproval(Long id, Long authShopId, String callerRole) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Kiểm tra quyền sở hữu Shop
        if (!isAdmin(callerRole)) {
            if (authShopId != null && !authShopId.equals(product.getShopId())) {
                throw new ForbiddenException("Bạn không có quyền nộp duyệt sản phẩm của gian hàng khác");
            }
        }

        if (product.getStatus().isApproved()) {
            throw new BadRequestException("Sản phẩm đã được phê duyệt và đang hoạt động trên hệ thống");
        }
        if (product.getStatus().isPending()) {
            throw new BadRequestException("Sản phẩm đang trong quá trình chờ kiểm duyệt");
        }

        product.setStatus(ProductStatus.PENDING);
        product.setRejectionReason(null);

        Product saved = productRepository.save(product);
        cacheService.evictProductCaches(id);

        log.info("Người bán shopId {} đã nộp duyệt sản phẩm ID {}", authShopId, id);
        return mapToDto(saved);
    }

    @Transactional
    public ProductDto updateProductStatus(Long id, ProductStatus newStatus, String adminNote, String callerRole) {
        // Kiểm tra quyền: Chỉ Admin mới có quyền duyệt / từ chối sản phẩm
        if (!isAdmin(callerRole)) {
            log.warn("Từ chối kiểm duyệt sản phẩm ID {}: Role {} không phải ROLE_ADMIN", id, callerRole);
            throw new ForbiddenException("Chỉ Quản trị viên hệ thống (ROLE_ADMIN) mới có quyền kiểm duyệt sản phẩm");
        }

        if (newStatus == ProductStatus.REJECTED) {
            if (adminNote == null || adminNote.isBlank()) {
                throw new BadRequestException("Vui lòng cung cấp lý do từ chối kiểm duyệt sản phẩm");
            }
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (newStatus == ProductStatus.REJECTED) {
            product.setStatus(ProductStatus.REJECTED);
            product.setRejectionReason(adminNote.trim());
        } else if (newStatus.isApproved()) {
            product.setStatus(ProductStatus.APPROVED);
            product.setRejectionReason(null);
        } else {
            product.setStatus(newStatus);
        }

        Product saved = productRepository.save(product);
        cacheService.evictProductCaches(id);

        log.info("Admin ({}) đã cập nhật trạng thái sản phẩm ID {} thành {}", callerRole, id, newStatus);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteProduct(Long id, Long authShopId, String callerRole) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Kiểm tra quyền sở hữu Shop
        if (!isAdmin(callerRole)) {
            if (authShopId != null && !authShopId.equals(product.getShopId())) {
                log.warn("Từ chối quyền xóa: caller shopId {} cố xóa sản phẩm của shopId {}", authShopId, product.getShopId());
                throw new ForbiddenException("Bạn không có quyền xóa sản phẩm thuộc gian hàng khác");
            }
        }

        variantRepository.deleteByProductId(id);
        productRepository.delete(product);
        cacheService.evictProductCaches(id);

        log.info("Đã xóa sản phẩm ID {} bởi shopId {} (Role: {})", id, authShopId, callerRole);
    }

    public CatalogVariantValidationDto getVariantValidation(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", variant.getProductId()));

        int stock = variant.getStock() != null ? variant.getStock() : 0;
        int reserved = variant.getReservedStock() != null ? variant.getReservedStock() : 0;
        int available = Math.max(0, stock - reserved);

        String variantName = String.format("%s - %s",
                variant.getColor() != null ? variant.getColor() : "",
                variant.getSize() != null ? variant.getSize() : "").trim();

        return CatalogVariantValidationDto.builder()
                .variantId(variant.getId())
                .productId(product.getId())
                .shopId(product.getShopId())
                .shopName("Gian Hàng " + product.getShopId())
                .productName(product.getName())
                .variantName(variantName.isEmpty() ? product.getName() : variantName)
                .sku(variant.getSku())
                .imageUrl(product.getMainImage())
                .price(variant.getPrice())
                .stock(stock)
                .reservedStock(reserved)
                .availableStock(available)
                .active(product.getStatus().isApproved())
                .build();
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

        // Tên danh mục
        String categoryName = "Danh mục chung";
        if (product.getCategoryId() != null) {
            categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName)
                    .orElse("Danh mục chung");
        }

        // Điểm đánh giá trung bình
        long reviewCount = reviewRepository.countByProductId(product.getId());
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());
        if (avgRating == null) {
            avgRating = 5.0;
        } else {
            avgRating = Math.round(avgRating * 10.0) / 10.0;
        }

        return ProductDto.builder()
                .id(product.getId())
                .shopId(product.getShopId())
                .shopName("Gian Hàng " + product.getShopId())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .name(product.getName())
                .description(product.getDescription())
                .mainImage(product.getMainImage())
                .status(product.getStatus())
                .rejectionReason(product.getRejectionReason())
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

    private boolean isAdmin(String role) {
        if (role == null) return false;
        return "ROLE_ADMIN".equalsIgnoreCase(role.trim()) || "ADMIN".equalsIgnoreCase(role.trim());
    }

    private boolean isBuyer(String role) {
        if (role == null) return false;
        return "ROLE_BUYER".equalsIgnoreCase(role.trim()) || "BUYER".equalsIgnoreCase(role.trim());
    }
}
