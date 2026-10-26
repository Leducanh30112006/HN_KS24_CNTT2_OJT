package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.CategoryDto;
import com.quickshop.catalog.dto.PageResult;
import com.quickshop.catalog.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10); // FR-03: TTL = 10 phút
    private static final String KEY_CATEGORIES_ALL = "catalog:categories:all";
    private static final String KEY_PRODUCT_PREFIX = "catalog:product:";
    private static final String KEY_PRODUCTS_PAGE_PREFIX = "catalog:products:filter:";

    @SuppressWarnings("unchecked")
    public List<CategoryDto> getCachedCategories() {
        try {
            Object data = redisTemplate.opsForValue().get(KEY_CATEGORIES_ALL);
            if (data instanceof List<?>) {
                log.debug("Redis Cache HIT for categories");
                return (List<CategoryDto>) data;
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for categories (Cache Miss fallback): {}", e.getMessage());
        }
        return null;
    }

    public void cacheCategories(List<CategoryDto> categories) {
        try {
            redisTemplate.opsForValue().set(KEY_CATEGORIES_ALL, categories, DEFAULT_TTL);
            log.debug("Cached categories to Redis with TTL 10m");
        } catch (Exception e) {
            log.warn("Redis write failed for categories: {}", e.getMessage());
        }
    }

    public ProductDto getCachedProduct(Long id) {
        try {
            Object data = redisTemplate.opsForValue().get(KEY_PRODUCT_PREFIX + id);
            if (data instanceof ProductDto) {
                log.debug("Redis Cache HIT for product id: {}", id);
                return (ProductDto) data;
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for product id {} : {}", id, e.getMessage());
        }
        return null;
    }

    public void cacheProduct(Long id, ProductDto product) {
        try {
            redisTemplate.opsForValue().set(KEY_PRODUCT_PREFIX + id, product, DEFAULT_TTL);
            log.debug("Cached product id {} to Redis with TTL 10m", id);
        } catch (Exception e) {
            log.warn("Redis write failed for product id {} : {}", id, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public PageResult<ProductDto> getCachedProductPage(String filterHash) {
        try {
            Object data = redisTemplate.opsForValue().get(KEY_PRODUCTS_PAGE_PREFIX + filterHash);
            if (data instanceof PageResult<?>) {
                log.debug("Redis Cache HIT for product filter page: {}", filterHash);
                return (PageResult<ProductDto>) data;
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for filter page {}: {}", filterHash, e.getMessage());
        }
        return null;
    }

    public void cacheProductPage(String filterHash, PageResult<ProductDto> pageResult) {
        try {
            redisTemplate.opsForValue().set(KEY_PRODUCTS_PAGE_PREFIX + filterHash, pageResult, DEFAULT_TTL);
            log.debug("Cached product page with hash {} to Redis with TTL 10m", filterHash);
        } catch (Exception e) {
            log.warn("Redis write failed for filter page {}: {}", filterHash, e.getMessage());
        }
    }

    public void evictProductCaches(Long productId) {
        try {
            if (productId != null) {
                redisTemplate.delete(KEY_PRODUCT_PREFIX + productId);
            }
            // Delete all filter pages
            Set<String> keys = redisTemplate.keys(KEY_PRODUCTS_PAGE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.info("Evicted product caches for product id: {} and search filter pages", productId);
        } catch (Exception e) {
            log.warn("Redis eviction failed for products: {}", e.getMessage());
        }
    }

    public void evictCategoryCaches() {
        try {
            redisTemplate.delete(KEY_CATEGORIES_ALL);
            Set<String> keys = redisTemplate.keys(KEY_PRODUCTS_PAGE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.info("Evicted category caches");
        } catch (Exception e) {
            log.warn("Redis eviction failed for categories: {}", e.getMessage());
        }
    }
}
