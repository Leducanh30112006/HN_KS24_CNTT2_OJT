package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.CategoryDto;
import com.quickshop.catalog.dto.PageResult;
import com.quickshop.catalog.dto.ProductDto;
import com.quickshop.catalog.entity.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogCacheService - Quản lý Redis Cache Danh sách/Chi tiết Sản phẩm, Cấu hình TTL & Cơ chế Invalidation")
class CatalogCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CatalogCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CatalogCacheService(redisTemplate);
        cacheService.setCacheEnabled(true);
        cacheService.setProductTtl(Duration.ofMinutes(10));
        cacheService.setProductsPageTtl(Duration.ofMinutes(5));
        cacheService.setCategoryTtl(Duration.ofMinutes(30));
    }

    // ================= CHI TIẾT SẢN PHẨM =================
    @Test
    @DisplayName("cacheProduct: Lưu chi tiết sản phẩm vào Redis với đúng TTL 10 phút")
    void testCacheProduct_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ProductDto dto = ProductDto.builder()
                .id(101L)
                .name("Tai nghe QuickPods Pro")
                .minPrice(new BigDecimal("1500000"))
                .status(ProductStatus.APPROVED)
                .build();

        cacheService.cacheProduct(101L, dto);

        verify(valueOperations).set("catalog:product:101", dto, Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("getCachedProduct: Cache HIT trả về đúng DTO của sản phẩm")
    void testGetCachedProduct_Hit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ProductDto dto = ProductDto.builder()
                .id(101L)
                .name("Tai nghe QuickPods Pro")
                .minPrice(new BigDecimal("1500000"))
                .status(ProductStatus.APPROVED)
                .build();

        when(valueOperations.get("catalog:product:101")).thenReturn(dto);

        ProductDto result = cacheService.getCachedProduct(101L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getName()).isEqualTo("Tai nghe QuickPods Pro");
    }

    @Test
    @DisplayName("getCachedProduct: Cache MISS trả về null")
    void testGetCachedProduct_Miss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catalog:product:999")).thenReturn(null);

        ProductDto result = cacheService.getCachedProduct(999L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getCachedProduct: Khi Redis lỗi kết nối, tự động fallback trả về null mà không throw Exception")
    void testGetCachedProduct_RedisException_GracefulFallback() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("Connection refused"));

        ProductDto result = cacheService.getCachedProduct(101L);

        assertThat(result).isNull();
    }

    // ================= DANH SÁCH & BỘ LỌC SẢN PHẨM =================
    @Test
    @DisplayName("cacheProductPage: Lưu trang sản phẩm vào Redis với TTL 5 phút")
    void testCacheProductPage_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PageResult<ProductDto> pageResult = PageResult.<ProductDto>builder()
                .content(List.of(ProductDto.builder().id(101L).name("SP 1").build()))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();

        String hash = "kw_phone_cat_1_p_0_s_10";
        cacheService.cacheProductPage(hash, pageResult);

        verify(valueOperations).set("catalog:products:filter:" + hash, pageResult, Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("getCachedProductPage: Cache HIT trả về trang sản phẩm từ Redis")
    void testGetCachedProductPage_Hit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PageResult<ProductDto> pageResult = PageResult.<ProductDto>builder()
                .content(List.of(ProductDto.builder().id(101L).name("SP 1").build()))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();

        String hash = "kw_phone_cat_1_p_0_s_10";
        when(valueOperations.get("catalog:products:filter:" + hash)).thenReturn(pageResult);

        PageResult<ProductDto> result = cacheService.getCachedProductPage(hash);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    // ================= CƠ CHẾ INVALIDATE / EVICT =================
    @Test
    @DisplayName("evictProductCaches: Xóa chính xác key sản phẩm và toàn bộ các trang filter/search")
    void testEvictProductCaches_Success() {
        Set<String> filterKeys = Set.of(
                "catalog:products:filter:page_0",
                "catalog:products:filter:page_1"
        );

        when(redisTemplate.delete("catalog:product:101")).thenReturn(true);
        when(redisTemplate.keys("catalog:products:filter:*")).thenReturn(filterKeys);
        when(redisTemplate.delete(filterKeys)).thenReturn(2L);

        cacheService.evictProductCaches(101L);

        verify(redisTemplate).delete("catalog:product:101");
        verify(redisTemplate).keys("catalog:products:filter:*");
        verify(redisTemplate).delete(filterKeys);
    }

    @Test
    @DisplayName("evictAllProductPages: Xóa toàn bộ các key trang filter")
    void testEvictAllProductPages_Success() {
        Set<String> filterKeys = Set.of("catalog:products:filter:page_0");
        when(redisTemplate.keys("catalog:products:filter:*")).thenReturn(filterKeys);
        when(redisTemplate.delete(filterKeys)).thenReturn(1L);

        cacheService.evictAllProductPages();

        verify(redisTemplate).delete(filterKeys);
    }

    // ================= CACHE DANH MỤC =================
    @Test
    @DisplayName("cacheCategories & getCachedCategories: Lưu và lấy danh mục với TTL 30 phút")
    void testCacheCategories_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        List<CategoryDto> categories = List.of(
                CategoryDto.builder().id(1L).name("Điện tử").slug("dien-tu").build()
        );

        cacheService.cacheCategories(categories);
        verify(valueOperations).set("catalog:categories:all", categories, Duration.ofMinutes(30));

        when(valueOperations.get("catalog:categories:all")).thenReturn(categories);
        List<CategoryDto> result = cacheService.getCachedCategories();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Điện tử");
    }

    @Test
    @DisplayName("evictCategoryCaches: Xóa cache danh mục all, tree và toàn bộ các trang sản phẩm")
    void testEvictCategoryCaches_Success() {
        Set<String> filterKeys = Set.of("catalog:products:filter:page_0");
        when(redisTemplate.keys("catalog:products:filter:*")).thenReturn(filterKeys);

        cacheService.evictCategoryCaches();

        verify(redisTemplate).delete("catalog:categories:all");
        verify(redisTemplate).delete("catalog:categories:tree");
        verify(redisTemplate).delete(filterKeys);
    }

    // ================= CLEAR TOÀN BỘ & TẮT CACHE =================
    @Test
    @DisplayName("clearAllCaches: Xóa toàn bộ keys namespace catalog:*")
    void testClearAllCaches_Success() {
        Set<String> allKeys = Set.of("catalog:categories:all", "catalog:product:1", "catalog:products:filter:hash");
        when(redisTemplate.keys("catalog:*")).thenReturn(allKeys);
        when(redisTemplate.delete(allKeys)).thenReturn(3L);

        cacheService.clearAllCaches();

        verify(redisTemplate).delete(allKeys);
    }

    @Test
    @DisplayName("cacheEnabled = false: Bỏ qua hoàn toàn Redis cache")
    void testCacheDisabled_BypassesCache() {
        cacheService.setCacheEnabled(false);

        ProductDto dto = ProductDto.builder().id(101L).name("SP").build();
        cacheService.cacheProduct(101L, dto);
        ProductDto result = cacheService.getCachedProduct(101L);

        assertThat(result).isNull();
        verifyNoInteractions(redisTemplate);
    }
}

