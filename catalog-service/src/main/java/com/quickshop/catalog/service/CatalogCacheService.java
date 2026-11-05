package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.CategoryDto;
import com.quickshop.catalog.dto.PageResult;
import com.quickshop.catalog.dto.ProductDto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Getter
@Setter
public class CatalogCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${catalog.cache.enabled:true}")
    private boolean cacheEnabled = true;

    @Value("${catalog.cache.product-ttl:600s}")
    private Duration productTtl = Duration.ofMinutes(10); // TTL 10 phút cho chi tiết sản phẩm

    @Value("${catalog.cache.products-page-ttl:300s}")
    private Duration productsPageTtl = Duration.ofMinutes(5); // TTL 5 phút cho danh sách tìm kiếm / bộ lọc

    @Value("${catalog.cache.category-ttl:1800s}")
    private Duration categoryTtl = Duration.ofMinutes(30); // TTL 30 phút cho danh mục sản phẩm

    public static final String KEY_CATEGORIES_ALL = "catalog:categories:all";
    public static final String KEY_CATEGORIES_TREE = "catalog:categories:tree";
    public static final String KEY_PRODUCT_PREFIX = "catalog:product:";
    public static final String KEY_PRODUCTS_PAGE_PREFIX = "catalog:products:filter:";

    public CatalogCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ================= CATEGORIES CACHE =================
    @SuppressWarnings("unchecked")
    public List<CategoryDto> getCachedCategories() {
        if (!cacheEnabled) return null;
        try {
            Object data = redisTemplate.opsForValue().get(KEY_CATEGORIES_ALL);
            if (data instanceof List<?>) {
                log.debug("Redis Cache HIT cho danh mục tất cả (all categories)");
                return (List<CategoryDto>) data;
            }
        } catch (Exception e) {
            log.warn("Redis lookup thất bại cho categories (Fallback DB): {}", e.getMessage());
        }
        return null;
    }

    public void cacheCategories(List<CategoryDto> categories) {
        if (!cacheEnabled || categories == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_CATEGORIES_ALL, categories, categoryTtl);
            log.debug("Lưu cache categories vào Redis với TTL {}", categoryTtl);
        } catch (Exception e) {
            log.warn("Ghi cache categories thất bại: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<CategoryDto> getCachedCategoryTree() {
        if (!cacheEnabled) return null;
        try {
            Object data = redisTemplate.opsForValue().get(KEY_CATEGORIES_TREE);
            if (data instanceof List<?>) {
                log.debug("Redis Cache HIT cho cây danh mục (category tree)");
                return (List<CategoryDto>) data;
            }
        } catch (Exception e) {
            log.warn("Redis lookup thất bại cho category tree: {}", e.getMessage());
        }
        return null;
    }

    public void cacheCategoryTree(List<CategoryDto> tree) {
        if (!cacheEnabled || tree == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_CATEGORIES_TREE, tree, categoryTtl);
            log.debug("Lưu cache category tree vào Redis với TTL {}", categoryTtl);
        } catch (Exception e) {
            log.warn("Ghi cache category tree thất bại: {}", e.getMessage());
        }
    }

    // ================= SINGLE PRODUCT CACHE =================
    public ProductDto getCachedProduct(Long id) {
        if (!cacheEnabled || id == null) return null;
        try {
            Object data = redisTemplate.opsForValue().get(KEY_PRODUCT_PREFIX + id);
            if (data instanceof ProductDto) {
                log.debug("Redis Cache HIT cho chi tiết sản phẩm ID: {}", id);
                return (ProductDto) data;
            }
        } catch (Exception e) {
            log.warn("Redis lookup thất bại cho sản phẩm ID {}: {}", id, e.getMessage());
        }
        return null;
    }

    public void cacheProduct(Long id, ProductDto product) {
        if (!cacheEnabled || id == null || product == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_PRODUCT_PREFIX + id, product, productTtl);
            log.debug("Lưu cache chi tiết sản phẩm ID {} vào Redis với TTL {}", id, productTtl);
        } catch (Exception e) {
            log.warn("Ghi cache chi tiết sản phẩm ID {} thất bại: {}", id, e.getMessage());
        }
    }

    // ================= PRODUCT LIST & FILTER PAGE CACHE =================
    @SuppressWarnings("unchecked")
    public PageResult<ProductDto> getCachedProductPage(String filterHash) {
        if (!cacheEnabled || filterHash == null) return null;
        try {
            Object data = redisTemplate.opsForValue().get(KEY_PRODUCTS_PAGE_PREFIX + filterHash);
            if (data instanceof PageResult<?>) {
                log.debug("Redis Cache HIT cho danh sách sản phẩm với hash: {}", filterHash);
                return (PageResult<ProductDto>) data;
            }
        } catch (Exception e) {
            log.warn("Redis lookup thất bại cho trang sản phẩm hash {}: {}", filterHash, e.getMessage());
        }
        return null;
    }

    public void cacheProductPage(String filterHash, PageResult<ProductDto> pageResult) {
        if (!cacheEnabled || filterHash == null || pageResult == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_PRODUCTS_PAGE_PREFIX + filterHash, pageResult, productsPageTtl);
            log.debug("Lưu cache danh sách sản phẩm hash {} vào Redis với TTL {}", filterHash, productsPageTtl);
        } catch (Exception e) {
            log.warn("Ghi cache danh sách sản phẩm hash {} thất bại: {}", filterHash, e.getMessage());
        }
    }

    // ================= CACHE INVALIDATION / EVICTION =================
    /**
     * Invalidate toàn bộ cache liên quan đến 1 sản phẩm:
     * - Cache chi tiết sản phẩm: catalog:product:{productId}
     * - Toàn bộ các trang danh sách/tìm kiếm/lọc: catalog:products:filter:*
     */
    public void evictProductCaches(Long productId) {
        if (!cacheEnabled) return;
        try {
            int deletedCount = 0;
            if (productId != null) {
                Boolean deleted = redisTemplate.delete(KEY_PRODUCT_PREFIX + productId);
                if (Boolean.TRUE.equals(deleted)) deletedCount++;
            }

            // Xóa toàn bộ các trang lọc sản phẩm (search/filter pages)
            Set<String> filterKeys = redisTemplate.keys(KEY_PRODUCTS_PAGE_PREFIX + "*");
            if (filterKeys != null && !filterKeys.isEmpty()) {
                Long keysDeleted = redisTemplate.delete(filterKeys);
                deletedCount += (keysDeleted != null ? keysDeleted.intValue() : 0);
            }

            log.info("Invalidate cache thành công cho sản phẩm ID {} (Đã xóa {} keys trong Redis)", productId, deletedCount);
        } catch (Exception e) {
            log.warn("Invalidate cache sản phẩm ID {} thất bại: {}", productId, e.getMessage());
        }
    }

    /**
     * Invalidate toàn bộ các trang phân trang/tìm kiếm
     */
    public void evictAllProductPages() {
        if (!cacheEnabled) return;
        try {
            Set<String> filterKeys = redisTemplate.keys(KEY_PRODUCTS_PAGE_PREFIX + "*");
            if (filterKeys != null && !filterKeys.isEmpty()) {
                Long deleted = redisTemplate.delete(filterKeys);
                log.info("Invalidate toàn bộ {} trang cache danh sách sản phẩm", deleted);
            }
        } catch (Exception e) {
            log.warn("Invalidate cache danh sách sản phẩm thất bại: {}", e.getMessage());
        }
    }

    /**
     * Invalidate cache danh mục và các trang sản phẩm liên quan
     */
    public void evictCategoryCaches() {
        if (!cacheEnabled) return;
        try {
            redisTemplate.delete(KEY_CATEGORIES_ALL);
            redisTemplate.delete(KEY_CATEGORIES_TREE);

            // Khi danh mục thay đổi cấu trúc, các trang lọc sản phẩm cũng cần được làm mới
            Set<String> filterKeys = redisTemplate.keys(KEY_PRODUCTS_PAGE_PREFIX + "*");
            if (filterKeys != null && !filterKeys.isEmpty()) {
                redisTemplate.delete(filterKeys);
            }
            log.info("Invalidate toàn bộ cache danh mục (all, tree) và các trang sản phẩm");
        } catch (Exception e) {
            log.warn("Invalidate cache danh mục thất bại: {}", e.getMessage());
        }
    }

    /**
     * Xóa sạch toàn bộ cache Catalog
     */
    public void clearAllCaches() {
        if (!cacheEnabled) return;
        try {
            Set<String> allCatalogKeys = redisTemplate.keys("catalog:*");
            if (allCatalogKeys != null && !allCatalogKeys.isEmpty()) {
                Long count = redisTemplate.delete(allCatalogKeys);
                log.info("Đã xóa sạch {} keys trong Redis catalog namespace", count);
            }
        } catch (Exception e) {
            log.warn("Xóa sạch cache catalog thất bại: {}", e.getMessage());
        }
    }
}
