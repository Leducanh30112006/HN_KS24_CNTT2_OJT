package com.quickshop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.client.CatalogServiceClient;
import com.quickshop.order.dto.cart.*;
import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;
import com.quickshop.order.exception.CartLockException;
import com.quickshop.order.service.impl.CartServiceImpl;
import com.quickshop.order.util.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private CatalogServiceClient catalogServiceClient;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    private ObjectMapper objectMapper;
    private CartServiceImpl cartService;

    private static final Long USER_ID = 1001L;
    private static final Long VARIANT_ID_1 = 201L;
    private static final Long VARIANT_ID_2 = 202L;
    private static final String CART_KEY = "cart:1001";
    private static final String LOCK_KEY = "lock:cart:1001";
    private static final String LOCK_TOKEN = "test-uuid-lock-token";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        cartService = new CartServiceImpl(
                stringRedisTemplate,
                objectMapper,
                catalogServiceClient,
                redisDistributedLock
        );
    }

    @Test
    @DisplayName("Thêm sản phẩm mới vào giỏ hàng thành công")
    void testAddToCart_NewItem_Success() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(VARIANT_ID_1)
                .quantity(2)
                .build();

        CatalogVariantValidationDto catalogDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(501L)
                .shopId(10L)
                .shopName("Shop Công Nghệ")
                .productName("Điện thoại X")
                .variantName("Đen 128GB")
                .sku("SKU-PHONE-X-BLACK")
                .imageUrl("https://example.com/phone.png")
                .price(BigDecimal.valueOf(15000000))
                .stock(50)
                .reservedStock(5)
                .availableStock(45)
                .active(true)
                .build();

        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(CART_KEY, String.valueOf(VARIANT_ID_1))).thenReturn(null);
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(catalogDto));

        // Mock cho hàm getCart trả về sau khi lưu
        CartItemRedisDto savedDto = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(501L)
                .shopId(10L)
                .shopName("Shop Công Nghệ")
                .productName("Điện thoại X")
                .variantName("Đen 128GB")
                .sku("SKU-PHONE-X-BLACK")
                .imageUrl("https://example.com/phone.png")
                .price(BigDecimal.valueOf(15000000))
                .quantity(2)
                .availableStock(45)
                .build();

        Map<Object, Object> cartEntries = new HashMap<>();
        cartEntries.put(String.valueOf(VARIANT_ID_1), objectMapper.writeValueAsString(savedDto));
        when(hashOperations.entries(CART_KEY)).thenReturn(cartEntries);

        CartResponse response = cartService.addToCart(USER_ID, request);

        assertNotNull(response);
        assertEquals(USER_ID, response.getUserId());
        assertEquals(1, response.getShops().size());
        assertEquals(1, response.getTotalItems());
        assertEquals(2, response.getTotalQuantity());
        assertEquals(BigDecimal.valueOf(30000000), response.getTotalAmount());

        CartShopGroupResponse shopGroup = response.getShops().get(0);
        assertEquals(10L, shopGroup.getShopId());
        assertEquals("Shop Công Nghệ", shopGroup.getShopName());
        assertEquals(1, shopGroup.getItems().size());
        assertEquals(BigDecimal.valueOf(30000000), shopGroup.getShopSubtotal());

        verify(hashOperations).put(eq(CART_KEY), eq(String.valueOf(VARIANT_ID_1)), anyString());
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Thêm sản phẩm đã có trong giỏ -> Cộng dồn số lượng chính xác")
    void testAddToCart_ExistingItem_IncrementsQuantity() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(VARIANT_ID_1)
                .quantity(3)
                .build();

        CartItemRedisDto currentDto = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(501L)
                .shopId(10L)
                .shopName("Shop Công Nghệ")
                .productName("Điện thoại X")
                .price(BigDecimal.valueOf(1000000))
                .quantity(2)
                .availableStock(20)
                .build();

        CatalogVariantValidationDto catalogDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(501L)
                .shopId(10L)
                .shopName("Shop Công Nghệ")
                .productName("Điện thoại X")
                .price(BigDecimal.valueOf(1000000))
                .availableStock(20)
                .active(true)
                .build();

        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(CART_KEY, String.valueOf(VARIANT_ID_1)))
                .thenReturn(objectMapper.writeValueAsString(currentDto));
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(catalogDto));

        CartItemRedisDto updatedDto = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .shopId(10L)
                .price(BigDecimal.valueOf(1000000))
                .quantity(5) // 2 + 3 = 5
                .availableStock(20)
                .build();
        Map<Object, Object> cartEntries = Map.of(String.valueOf(VARIANT_ID_1), objectMapper.writeValueAsString(updatedDto));
        when(hashOperations.entries(CART_KEY)).thenReturn(cartEntries);

        CartResponse response = cartService.addToCart(USER_ID, request);

        assertEquals(5, response.getTotalQuantity());
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Thêm số lượng vượt quá tồn kho khả dụng -> Ném lỗi BadRequestException")
    void testAddToCart_ExceedsAvailableStock_ThrowsBadRequestException() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(VARIANT_ID_1)
                .quantity(15)
                .build();

        CatalogVariantValidationDto catalogDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productName("Áo thun")
                .availableStock(10) // Tồn kho chỉ có 10 mà yêu cầu 15
                .active(true)
                .build();

        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(CART_KEY, String.valueOf(VARIANT_ID_1))).thenReturn(null);
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(catalogDto));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cartService.addToCart(USER_ID, request)
        );

        assertTrue(ex.getMessage().contains("vượt quá tồn kho khả dụng"));
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Thêm biến thể không tồn tại trong Catalog -> Ném ResourceNotFoundException")
    void testAddToCart_VariantNotFound_ThrowsResourceNotFoundException() {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(999L)
                .quantity(1)
                .build();

        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(CART_KEY, "999")).thenReturn(null);
        when(catalogServiceClient.getVariantDetails(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.addToCart(USER_ID, request)
        );

        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Thêm sản phẩm đã ngừng kinh doanh -> Ném BadRequestException")
    void testAddToCart_VariantInactive_ThrowsBadRequestException() {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(VARIANT_ID_1)
                .quantity(1)
                .build();

        CatalogVariantValidationDto catalogDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productName("Sản phẩm khóa")
                .availableStock(50)
                .active(false) // Không active
                .build();

        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(CART_KEY, String.valueOf(VARIANT_ID_1))).thenReturn(null);
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(catalogDto));

        assertThrows(BadRequestException.class, () ->
                cartService.addToCart(USER_ID, request)
        );

        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Không chiếm được khóa phân tán Redis -> Ném CartLockException (bảo vệ đồng thời)")
    void testAddToCart_CannotAcquireLock_ThrowsCartLockException() {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(VARIANT_ID_1)
                .quantity(1)
                .build();

        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(null); // Không lấy được lock do có request khác đang giữ

        assertThrows(CartLockException.class, () ->
                cartService.addToCart(USER_ID, request)
        );

        verifyNoInteractions(catalogServiceClient);
    }

    @Test
    @DisplayName("Cập nhật số lượng sản phẩm trong giỏ thành công")
    void testUpdateCartItemQuantity_Success() throws Exception {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(4)
                .build();

        CartItemRedisDto currentDto = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .shopId(10L)
                .price(BigDecimal.valueOf(200000))
                .quantity(2)
                .availableStock(10)
                .build();

        CatalogVariantValidationDto catalogDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .price(BigDecimal.valueOf(200000))
                .availableStock(10)
                .active(true)
                .build();

        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(CART_KEY, String.valueOf(VARIANT_ID_1)))
                .thenReturn(objectMapper.writeValueAsString(currentDto));
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(catalogDto));

        CartItemRedisDto updatedDto = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .shopId(10L)
                .price(BigDecimal.valueOf(200000))
                .quantity(4)
                .availableStock(10)
                .build();
        Map<Object, Object> cartEntries = Map.of(String.valueOf(VARIANT_ID_1), objectMapper.writeValueAsString(updatedDto));
        when(hashOperations.entries(CART_KEY)).thenReturn(cartEntries);

        CartResponse response = cartService.updateCartItemQuantity(USER_ID, VARIANT_ID_1, request);

        assertEquals(4, response.getTotalQuantity());
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Cập nhật số lượng = 0 -> Tự động xóa khỏi giỏ hàng")
    void testUpdateCartItemQuantity_ZeroQuantity_RemovesItem() throws Exception {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(0)
                .build();

        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(CART_KEY, String.valueOf(VARIANT_ID_1))).thenReturn(1L);
        when(hashOperations.size(CART_KEY)).thenReturn(0L);
        when(hashOperations.entries(CART_KEY)).thenReturn(Collections.emptyMap());

        CartResponse response = cartService.updateCartItemQuantity(USER_ID, VARIANT_ID_1, request);

        assertEquals(0, response.getTotalItems());
        assertEquals(0, response.getTotalQuantity());
        verify(stringRedisTemplate).delete(CART_KEY);
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Xóa sản phẩm khỏi giỏ hàng thành công")
    void testRemoveCartItem_Success() {
        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(CART_KEY, String.valueOf(VARIANT_ID_1))).thenReturn(1L);
        when(hashOperations.size(CART_KEY)).thenReturn(0L);
        when(hashOperations.entries(CART_KEY)).thenReturn(Collections.emptyMap());

        CartResponse response = cartService.removeCartItem(USER_ID, VARIANT_ID_1);

        assertNotNull(response);
        assertEquals(0, response.getTotalItems());
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Xóa mặt hàng không có trong giỏ -> Ném ResourceNotFoundException")
    void testRemoveCartItem_NotFound_ThrowsResourceNotFoundException() {
        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(CART_KEY, String.valueOf(VARIANT_ID_1))).thenReturn(0L);

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeCartItem(USER_ID, VARIANT_ID_1)
        );

        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Lấy giỏ hàng -> Gom nhóm đa Shop và tính toán tổng tiền chuẩn xác")
    void testGetCart_GroupsByShop_CalculatesTotalsCorrectly() throws Exception {
        CartItemRedisDto item1 = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(10L)
                .shopId(1L)
                .shopName("Shop Thời Trang A")
                .productName("Áo Sơ Mi")
                .price(BigDecimal.valueOf(200000))
                .quantity(2) // 400.000
                .availableStock(10)
                .build();

        CartItemRedisDto item2 = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_2)
                .productId(20L)
                .shopId(2L)
                .shopName("Shop Điện Tử B")
                .productName("Tai Nghe")
                .price(BigDecimal.valueOf(500000))
                .quantity(1) // 500.000
                .availableStock(5)
                .build();

        Map<Object, Object> cartEntries = new HashMap<>();
        cartEntries.put(String.valueOf(VARIANT_ID_1), objectMapper.writeValueAsString(item1));
        cartEntries.put(String.valueOf(VARIANT_ID_2), objectMapper.writeValueAsString(item2));

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(CART_KEY)).thenReturn(cartEntries);

        CartResponse response = cartService.getCart(USER_ID);

        assertNotNull(response);
        assertEquals(USER_ID, response.getUserId());
        assertEquals(2, response.getShops().size(), "Phải gom nhóm thành 2 Shop riêng biệt");
        assertEquals(2, response.getTotalItems(), "Tổng số item là 2");
        assertEquals(3, response.getTotalQuantity(), "Tổng số lượng sản phẩm là 3");
        assertEquals(BigDecimal.valueOf(900000), response.getTotalAmount(), "Tổng tiền phải là 900.000 VND");

        CartShopGroupResponse shop1 = response.getShops().stream()
                .filter(s -> s.getShopId().equals(1L))
                .findFirst().orElseThrow();
        assertEquals("Shop Thời Trang A", shop1.getShopName());
        assertEquals(BigDecimal.valueOf(400000), shop1.getShopSubtotal());

        CartShopGroupResponse shop2 = response.getShops().stream()
                .filter(s -> s.getShopId().equals(2L))
                .findFirst().orElseThrow();
        assertEquals("Shop Điện Tử B", shop2.getShopName());
        assertEquals(BigDecimal.valueOf(500000), shop2.getShopSubtotal());
    }

    @Test
    @DisplayName("Lấy giỏ hàng khi trống -> Trả về cấu trúc giỏ rỗng với số lượng 0")
    void testGetCart_EmptyCart_ReturnsZeroTotals() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(CART_KEY)).thenReturn(Collections.emptyMap());

        CartResponse response = cartService.getCart(USER_ID);

        assertNotNull(response);
        assertEquals(0, response.getShops().size());
        assertEquals(0, response.getTotalItems());
        assertEquals(0, response.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
    }

    @Test
    @DisplayName("Lấy giỏ hàng khi gặp JSON hỏng -> Tự động xóa rác và giữ lại các item hợp lệ")
    void testGetCart_CorruptedJsonEntry_SkipsAndCleansCorruptedEntry() throws Exception {
        CartItemRedisDto validItem = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .shopId(1L)
                .shopName("Shop Chuẩn")
                .productName("Chuột không dây")
                .price(BigDecimal.valueOf(150000))
                .quantity(1)
                .availableStock(10)
                .build();

        Map<Object, Object> cartEntries = new HashMap<>();
        cartEntries.put(String.valueOf(VARIANT_ID_1), objectMapper.writeValueAsString(validItem));
        cartEntries.put("999", "{invalid_corrupted_json_value"); // Dữ liệu rác

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(CART_KEY)).thenReturn(cartEntries);

        CartResponse response = cartService.getCart(USER_ID);

        assertNotNull(response);
        assertEquals(1, response.getTotalItems(), "Phải giữ lại 1 item hợp lệ");
        assertEquals(BigDecimal.valueOf(150000), response.getTotalAmount());
        verify(hashOperations).delete(CART_KEY, "999");
    }

    @Test
    @DisplayName("Làm trống giỏ hàng thành công")
    void testClearCart_Success() {
        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);

        cartService.clearCart(USER_ID);

        verify(stringRedisTemplate).delete(CART_KEY);
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Merge Cart: Thêm mới hoàn toàn khi variant chưa có trong User Cart")
    void testMergeCart_NewVariants_AddsAllSuccessfully() throws Exception {
        GuestCartItemDto guestItem1 = GuestCartItemDto.builder()
                .variantId(VARIANT_ID_1)
                .quantity(2)
                .build();
        GuestCartItemDto guestItem2 = GuestCartItemDto.builder()
                .variantId(VARIANT_ID_2)
                .quantity(1)
                .build();

        MergeCartRequest mergeRequest = MergeCartRequest.builder()
                .requestId("req-merge-101")
                .items(List.of(guestItem1, guestItem2))
                .build();

        CatalogVariantValidationDto catalog1 = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(1L)
                .shopId(10L)
                .shopName("Shop 10")
                .productName("Sản phẩm 1")
                .price(BigDecimal.valueOf(100000))
                .availableStock(20)
                .active(true)
                .build();

        CatalogVariantValidationDto catalog2 = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_2)
                .productId(2L)
                .shopId(20L)
                .shopName("Shop 20")
                .productName("Sản phẩm 2")
                .price(BigDecimal.valueOf(200000))
                .availableStock(10)
                .active(true)
                .build();

        String idempotencyKey = "cart:merge:idempotency:" + USER_ID + ":req-merge-101";

        when(stringRedisTemplate.hasKey(idempotencyKey)).thenReturn(Boolean.FALSE);
        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(CART_KEY)).thenReturn(Collections.emptyMap()); // Giỏ user rỗng ban đầu

        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(catalog1));
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_2)).thenReturn(Optional.of(catalog2));

        MergeCartResponse response = cartService.mergeCart(USER_ID, mergeRequest);

        assertNotNull(response);
        assertFalse(response.isDuplicateRequest());
        assertEquals(2, response.getMergedItemsCount());
        assertTrue(response.getWarnings().isEmpty());

        verify(hashOperations).putAll(eq(CART_KEY), anyMap());
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Merge Cart: Cộng dồn số lượng chính xác khi variant đã tồn tại trong User Cart")
    void testMergeCart_ExistingVariants_IncrementsQuantities() throws Exception {
        GuestCartItemDto guestItem = GuestCartItemDto.builder()
                .variantId(VARIANT_ID_1)
                .quantity(3) // Khách có 3
                .build();

        MergeCartRequest mergeRequest = MergeCartRequest.builder()
                .requestId("req-merge-102")
                .items(List.of(guestItem))
                .build();

        // User đã có sẵn 2 trong giỏ
        CartItemRedisDto userExistingItem = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(1L)
                .shopId(10L)
                .shopName("Shop 10")
                .productName("Sản phẩm 1")
                .price(BigDecimal.valueOf(100000))
                .quantity(2)
                .availableStock(50)
                .build();

        CatalogVariantValidationDto catalogDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(1L)
                .shopId(10L)
                .shopName("Shop 10")
                .productName("Sản phẩm 1")
                .price(BigDecimal.valueOf(100000))
                .availableStock(50)
                .active(true)
                .build();

        String idempotencyKey = "cart:merge:idempotency:" + USER_ID + ":req-merge-102";

        when(stringRedisTemplate.hasKey(idempotencyKey)).thenReturn(Boolean.FALSE);
        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        Map<Object, Object> currentEntries = Map.of(
                String.valueOf(VARIANT_ID_1), objectMapper.writeValueAsString(userExistingItem)
        );
        when(hashOperations.entries(CART_KEY)).thenReturn(currentEntries);
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(catalogDto));

        MergeCartResponse response = cartService.mergeCart(USER_ID, mergeRequest);

        assertNotNull(response);
        assertEquals(1, response.getMergedItemsCount());
        assertTrue(response.getWarnings().isEmpty());

        // Kiểm tra dữ liệu được ghi vào Redis Hash: số lượng mới phải là 2 + 3 = 5
        verify(hashOperations).putAll(eq(CART_KEY), argThat(map -> {
            String json = (String) map.get(String.valueOf(VARIANT_ID_1));
            return json != null && json.contains("\"quantity\":5");
        }));
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Merge Cart: Vượt tồn kho khả dụng -> Điều chỉnh về tồn kho tối đa và sinh cảnh báo")
    void testMergeCart_ExceedsStock_AdjustsToMaxAvailableStockWithWarning() throws Exception {
        GuestCartItemDto guestItem = GuestCartItemDto.builder()
                .variantId(VARIANT_ID_1)
                .quantity(8) // Guest có 8
                .build();

        MergeCartRequest mergeRequest = MergeCartRequest.builder()
                .requestId("req-merge-103")
                .items(List.of(guestItem))
                .build();

        // User đã có sẵn 2 trong giỏ (tổng yêu cầu là 2 + 8 = 10)
        CartItemRedisDto userExistingItem = CartItemRedisDto.builder()
                .variantId(VARIANT_ID_1)
                .shopId(10L)
                .quantity(2)
                .availableStock(6)
                .build();

        // Nhưng kho chỉ còn 6 sản phẩm khả dụng
        CatalogVariantValidationDto catalogDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productName("Tai nghe Bluetooth")
                .price(BigDecimal.valueOf(300000))
                .availableStock(6)
                .active(true)
                .build();

        String idempotencyKey = "cart:merge:idempotency:" + USER_ID + ":req-merge-103";

        when(stringRedisTemplate.hasKey(idempotencyKey)).thenReturn(Boolean.FALSE);
        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        Map<Object, Object> currentEntries = Map.of(
                String.valueOf(VARIANT_ID_1), objectMapper.writeValueAsString(userExistingItem)
        );
        when(hashOperations.entries(CART_KEY)).thenReturn(currentEntries);
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(catalogDto));

        MergeCartResponse response = cartService.mergeCart(USER_ID, mergeRequest);

        assertNotNull(response);
        assertEquals(1, response.getMergedItemsCount());
        assertFalse(response.getWarnings().isEmpty());
        assertTrue(response.getWarnings().get(0).contains("chỉ còn 6 sản phẩm khả dụng"));

        // Xác nhận số lượng lưu trong Redis được điều chỉnh về tối đa là 6
        verify(hashOperations).putAll(eq(CART_KEY), argThat(map -> {
            String json = (String) map.get(String.valueOf(VARIANT_ID_1));
            return json != null && json.contains("\"quantity\":6");
        }));
        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }

    @Test
    @DisplayName("Merge Cart: Request lặp lại cùng requestId (Idempotency) -> Không cộng dồn hai lần")
    void testMergeCart_DuplicateRequest_Idempotent_DoesNotDoubleAdd() {
        GuestCartItemDto guestItem = GuestCartItemDto.builder()
                .variantId(VARIANT_ID_1)
                .quantity(2)
                .build();

        MergeCartRequest mergeRequest = MergeCartRequest.builder()
                .requestId("req-merge-duplicated")
                .items(List.of(guestItem))
                .build();

        String idempotencyKey = "cart:merge:idempotency:" + USER_ID + ":req-merge-duplicated";

        // Giả lập key idempotency đã tồn tại trong Redis từ lần gọi trước
        when(stringRedisTemplate.hasKey(idempotencyKey)).thenReturn(Boolean.TRUE);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(CART_KEY)).thenReturn(Collections.emptyMap());

        MergeCartResponse response = cartService.mergeCart(USER_ID, mergeRequest);

        assertNotNull(response);
        assertTrue(response.isDuplicateRequest());
        assertEquals(0, response.getMergedItemsCount());
        assertTrue(response.getWarnings().get(0).contains("Idempotent"));

        // Đảm bảo KHÔNG thực hiện putAll ghi đè lên Redis
        verify(hashOperations, never()).putAll(anyString(), anyMap());
        verifyNoInteractions(catalogServiceClient);
    }

    @Test
    @DisplayName("Merge Cart: Variant ngừng kinh doanh hoặc hết hàng -> Bỏ qua và sinh cảnh báo")
    void testMergeCart_InactiveOrOutOfStockVariant_SkipsAndGeneratesWarning() {
        GuestCartItemDto outOfStockItem = GuestCartItemDto.builder()
                .variantId(VARIANT_ID_1)
                .quantity(1)
                .build();
        GuestCartItemDto inactiveItem = GuestCartItemDto.builder()
                .variantId(VARIANT_ID_2)
                .quantity(1)
                .build();

        MergeCartRequest mergeRequest = MergeCartRequest.builder()
                .requestId("req-merge-invalid")
                .items(List.of(outOfStockItem, inactiveItem))
                .build();

        CatalogVariantValidationDto outOfStockCatalog = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productName("Sản phẩm hết hàng")
                .availableStock(0) // Hết hàng
                .active(true)
                .build();

        CatalogVariantValidationDto inactiveCatalog = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_2)
                .productName("Sản phẩm ngừng bán")
                .availableStock(10)
                .active(false) // Ngừng bán
                .build();

        String idempotencyKey = "cart:merge:idempotency:" + USER_ID + ":req-merge-invalid";

        when(stringRedisTemplate.hasKey(idempotencyKey)).thenReturn(Boolean.FALSE);
        when(redisDistributedLock.acquireLock(eq(LOCK_KEY), anyLong(), anyInt(), anyLong()))
                .thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(CART_KEY)).thenReturn(Collections.emptyMap());

        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(outOfStockCatalog));
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_2)).thenReturn(Optional.of(inactiveCatalog));

        MergeCartResponse response = cartService.mergeCart(USER_ID, mergeRequest);

        assertNotNull(response);
        assertEquals(0, response.getMergedItemsCount());
        assertEquals(2, response.getWarnings().size());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("hết hàng")));
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("ngừng kinh doanh")));

        verify(redisDistributedLock).releaseLock(LOCK_KEY, LOCK_TOKEN);
    }
}
