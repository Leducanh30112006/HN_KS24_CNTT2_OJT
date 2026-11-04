package com.quickshop.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.client.CatalogServiceClient;
import com.quickshop.order.dto.cart.*;
import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;
import com.quickshop.order.exception.CartLockException;
import com.quickshop.order.exception.CartOperationException;
import com.quickshop.order.service.CartService;
import com.quickshop.order.util.CartRedisKeyManager;
import com.quickshop.order.util.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CatalogServiceClient catalogServiceClient;
    private final RedisDistributedLock redisDistributedLock;
    private final CartRedisKeyManager cartRedisKeyManager;

    private static final String IDEMPOTENCY_KEY_PREFIX = "cart:merge:idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(15);

    // Thông số cấu hình Distributed Lock: timeout 3000ms, retry 5 lần, mỗi lần cách 50ms
    private static final long LOCK_TIMEOUT_MS = 3000L;
    private static final int LOCK_MAX_RETRIES = 5;
    private static final long LOCK_RETRY_INTERVAL_MS = 50L;

    // ==========================================
    // USER CART IMPLEMENTATION
    // ==========================================

    @Override
    public CartResponse getCart(Long userId) {
        validateUserId(userId);
        String cartKey = cartRedisKeyManager.resolveUserCartKey(userId);

        Map<Object, Object> rawEntries;
        try {
            rawEntries = stringRedisTemplate.opsForHash().entries(cartKey);
        } catch (Exception ex) {
            log.error("Lỗi khi đọc giỏ hàng từ Redis cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Lỗi hệ thống khi tải giỏ hàng từ bộ nhớ đệm", ex);
        }

        Long ttl = cartRedisKeyManager.getTtlSeconds(cartKey);

        if (rawEntries == null || rawEntries.isEmpty()) {
            return CartResponse.builder()
                    .userId(userId)
                    .cartType(CartType.USER)
                    .shops(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .totalQuantity(0)
                    .ttlSeconds(ttl != null && ttl > 0 ? ttl : CartRedisKeyManager.USER_CART_TTL.toSeconds())
                    .build();
        }

        List<CartItemResponse> validItems = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : rawEntries.entrySet()) {
            String field = String.valueOf(entry.getKey());
            String jsonVal = String.valueOf(entry.getValue());

            try {
                CartItemRedisDto dto = objectMapper.readValue(jsonVal, CartItemRedisDto.class);
                CartItemResponse itemResponse = toItemResponse(dto);
                validItems.add(itemResponse);
            } catch (Exception parseEx) {
                log.error("Dữ liệu JSON giỏ hàng bị hỏng tại field [{}] của user {}. Tự động dọn dẹp entry rác.", field, userId);
                try {
                    stringRedisTemplate.opsForHash().delete(cartKey, field);
                } catch (Exception deleteEx) {
                    log.warn("Không thể xóa field hỏng {} khỏi Redis: {}", field, deleteEx.getMessage());
                }
            }
        }

        CartResponse response = groupItemsByShop(userId, validItems);
        response.setTtlSeconds(ttl != null && ttl > 0 ? ttl : CartRedisKeyManager.USER_CART_TTL.toSeconds());
        return response;
    }

    @Override
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        validateUserId(userId);
        validateAddToCartRequest(request);

        Long variantId = request.getVariantId();
        String cartKey = cartRedisKeyManager.resolveUserCartKey(userId);
        String lockKey = cartRedisKeyManager.getUserLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang được cập nhật bởi một yêu cầu khác, vui lòng thử lại sau giây lát");
        }

        try {
            int existingQty = 0;
            String existingJson = (String) stringRedisTemplate.opsForHash().get(cartKey, String.valueOf(variantId));
            if (existingJson != null) {
                try {
                    CartItemRedisDto existingDto = objectMapper.readValue(existingJson, CartItemRedisDto.class);
                    existingQty = existingDto.getQuantity() != null ? existingDto.getQuantity() : 0;
                } catch (Exception parseEx) {
                    log.warn("Lỗi đọc JSON item cũ variantId {} cho user {}, sẽ ghi đè.", variantId, userId);
                }
            }

            int requestedNewQty = existingQty + request.getQuantity();
            if (requestedNewQty > 999) {
                throw new BadRequestException("Tổng số lượng cho sản phẩm này trong giỏ hàng không được vượt quá 999");
            }

            CatalogVariantValidationDto catalogDto = catalogServiceClient.getVariantDetails(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Biến thể sản phẩm (variantId=" + variantId + ") không tồn tại hoặc đã ngừng kinh doanh"));

            if (!catalogDto.isActive()) {
                throw new BadRequestException("Sản phẩm hoặc gian hàng hiện không còn hoạt động để thêm vào giỏ");
            }

            int availableStock = catalogDto.getAvailableStock() != null ? catalogDto.getAvailableStock() : 0;
            if (availableStock <= 0) {
                throw new BadRequestException("Biến thể sản phẩm này hiện đã hết hàng trong kho");
            }

            if (requestedNewQty > availableStock) {
                throw new BadRequestException(String.format(
                        "Số lượng yêu cầu (%d) vượt quá tồn kho khả dụng hiện tại (%d). Vui lòng giảm số lượng.",
                        requestedNewQty, availableStock
                ));
            }

            BigDecimal price = catalogDto.getPrice();
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(requestedNewQty));

            CartItemRedisDto updatedDto = CartItemRedisDto.builder()
                    .variantId(catalogDto.getVariantId())
                    .productId(catalogDto.getProductId())
                    .shopId(catalogDto.getShopId())
                    .shopName(catalogDto.getShopName() != null ? catalogDto.getShopName() : "Shop #" + catalogDto.getShopId())
                    .productName(catalogDto.getProductName())
                    .variantName(catalogDto.getVariantName())
                    .sku(catalogDto.getSku())
                    .imageUrl(catalogDto.getImageUrl())
                    .price(price)
                    .quantity(requestedNewQty)
                    .availableStock(availableStock)
                    .cartType(CartType.USER)
                    .ownerId(String.valueOf(userId))
                    .selected(true)
                    .itemSubtotal(itemSubtotal)
                    .updatedAt(LocalDateTime.now())
                    .build();

            stringRedisTemplate.opsForHash().put(cartKey, String.valueOf(variantId), objectMapper.writeValueAsString(updatedDto));
            cartRedisKeyManager.refreshTtl(cartKey, CartType.USER);

            log.info("Thêm thành công variantId {} (số lượng: {}) vào User Cart {}", variantId, requestedNewQty, userId);
            return getCart(userId);

        } catch (BadRequestException | ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi thêm sản phẩm vào User Cart cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Thêm sản phẩm vào giỏ hàng thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public CartResponse updateCartItemQuantity(Long userId, Long variantId, UpdateCartItemRequest request) {
        validateUserId(userId);
        if (variantId == null || variantId <= 0) {
            throw new BadRequestException("variantId không hợp lệ");
        }
        if (request == null || request.getQuantity() == null) {
            throw new BadRequestException("Thông tin cập nhật số lượng không được để trống");
        }
        if (request.getQuantity() < 0) {
            throw new BadRequestException("Số lượng không thể là số âm");
        }
        if (request.getQuantity() > 999) {
            throw new BadRequestException("Số lượng không được vượt quá 999");
        }

        if (request.getQuantity() == 0) {
            return removeCartItem(userId, variantId);
        }

        String cartKey = cartRedisKeyManager.resolveUserCartKey(userId);
        String lockKey = cartRedisKeyManager.getUserLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            String existingJson = (String) stringRedisTemplate.opsForHash().get(cartKey, String.valueOf(variantId));
            if (existingJson == null) {
                throw new ResourceNotFoundException("Sản phẩm (variantId=" + variantId + ") không có trong giỏ hàng");
            }

            CartItemRedisDto currentDto = objectMapper.readValue(existingJson, CartItemRedisDto.class);

            CatalogVariantValidationDto catalogDto = catalogServiceClient.getVariantDetails(variantId)
                    .orElse(null);

            int availableStock = (catalogDto != null && catalogDto.getAvailableStock() != null)
                    ? catalogDto.getAvailableStock()
                    : (currentDto.getAvailableStock() != null ? currentDto.getAvailableStock() : 999);

            if (request.getQuantity() > availableStock) {
                throw new BadRequestException(String.format(
                        "Số lượng cập nhật (%d) vượt quá tồn kho khả dụng hiện tại (%d)",
                        request.getQuantity(), availableStock
                ));
            }

            BigDecimal price = (catalogDto != null && catalogDto.getPrice() != null) ? catalogDto.getPrice() : currentDto.getPrice();
            currentDto.setQuantity(request.getQuantity());
            currentDto.setAvailableStock(availableStock);
            currentDto.setPrice(price);
            currentDto.setItemSubtotal(price.multiply(BigDecimal.valueOf(request.getQuantity())));
            currentDto.setUpdatedAt(LocalDateTime.now());

            stringRedisTemplate.opsForHash().put(cartKey, String.valueOf(variantId), objectMapper.writeValueAsString(currentDto));
            cartRedisKeyManager.refreshTtl(cartKey, CartType.USER);

            log.info("Cập nhật số lượng variantId {} thành {} cho User Cart {}", variantId, request.getQuantity(), userId);
            return getCart(userId);

        } catch (BadRequestException | ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi cập nhật số lượng cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Cập nhật giỏ hàng thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public CartResponse removeCartItem(Long userId, Long variantId) {
        validateUserId(userId);
        if (variantId == null || variantId <= 0) {
            throw new BadRequestException("variantId không hợp lệ");
        }

        String cartKey = cartRedisKeyManager.resolveUserCartKey(userId);
        String lockKey = cartRedisKeyManager.getUserLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            Long deleted = stringRedisTemplate.opsForHash().delete(cartKey, String.valueOf(variantId));
            if (deleted == null || deleted == 0) {
                throw new ResourceNotFoundException("Mặt hàng (variantId=" + variantId + ") không có trong giỏ hàng");
            }
            log.info("Đã xóa variantId {} khỏi User Cart {} (kết quả: {})", variantId, userId, deleted);

            Long remaining = stringRedisTemplate.opsForHash().size(cartKey);
            if (remaining != null && remaining == 0) {
                stringRedisTemplate.delete(cartKey);
            } else {
                cartRedisKeyManager.refreshTtl(cartKey, CartType.USER);
            }

            return getCart(userId);
        } catch (BadRequestException | ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi xóa mặt hàng {} khỏi giỏ cho user {}: {}", variantId, userId, ex.getMessage(), ex);
            throw new CartOperationException("Xóa mặt hàng khỏi giỏ hàng thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public void clearCart(Long userId) {
        validateUserId(userId);
        String cartKey = cartRedisKeyManager.resolveUserCartKey(userId);
        String lockKey = cartRedisKeyManager.getUserLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            stringRedisTemplate.delete(cartKey);
            log.info("Đã làm trống toàn bộ User Cart {}", userId);
        } catch (Exception ex) {
            log.error("Lỗi khi làm trống giỏ hàng cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Làm trống giỏ hàng thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    // ==========================================
    // GUEST CART IMPLEMENTATION (REDIS TTL 7 DAYS)
    // ==========================================

    @Override
    public CartResponse getGuestCart(String guestId) {
        validateGuestId(guestId);
        String cartKey = cartRedisKeyManager.getGuestCartKey(guestId);

        Map<Object, Object> rawEntries;
        try {
            rawEntries = stringRedisTemplate.opsForHash().entries(cartKey);
        } catch (Exception ex) {
            log.error("Lỗi khi đọc giỏ hàng từ Redis cho guest {}: {}", guestId, ex.getMessage(), ex);
            throw new CartOperationException("Lỗi hệ thống khi tải giỏ hàng khách vãng lai", ex);
        }

        Long ttl = cartRedisKeyManager.getTtlSeconds(cartKey);

        if (rawEntries == null || rawEntries.isEmpty()) {
            return CartResponse.builder()
                    .guestId(guestId)
                    .cartType(CartType.GUEST)
                    .shops(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .totalQuantity(0)
                    .ttlSeconds(ttl != null && ttl > 0 ? ttl : CartRedisKeyManager.GUEST_CART_TTL.toSeconds())
                    .build();
        }

        List<CartItemResponse> validItems = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : rawEntries.entrySet()) {
            String field = String.valueOf(entry.getKey());
            String jsonVal = String.valueOf(entry.getValue());

            try {
                CartItemRedisDto dto = objectMapper.readValue(jsonVal, CartItemRedisDto.class);
                CartItemResponse itemResponse = toItemResponse(dto);
                validItems.add(itemResponse);
            } catch (Exception parseEx) {
                log.error("Dữ liệu JSON rác tại field [{}] của guest {}. Dọn dẹp entry.", field, guestId);
                try {
                    stringRedisTemplate.opsForHash().delete(cartKey, field);
                } catch (Exception deleteEx) {
                    log.warn("Không thể xóa field rác: {}", deleteEx.getMessage());
                }
            }
        }

        CartResponse response = groupItemsByShopForGuest(guestId, validItems);
        response.setTtlSeconds(ttl != null && ttl > 0 ? ttl : CartRedisKeyManager.GUEST_CART_TTL.toSeconds());
        return response;
    }

    @Override
    public CartResponse addToGuestCart(String guestId, AddToCartRequest request) {
        validateGuestId(guestId);
        validateAddToCartRequest(request);

        Long variantId = request.getVariantId();
        String cartKey = cartRedisKeyManager.getGuestCartKey(guestId);
        String lockKey = cartRedisKeyManager.getGuestLockKey(guestId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng khách đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            int existingQty = 0;
            String existingJson = (String) stringRedisTemplate.opsForHash().get(cartKey, String.valueOf(variantId));
            if (existingJson != null) {
                try {
                    CartItemRedisDto existingDto = objectMapper.readValue(existingJson, CartItemRedisDto.class);
                    existingQty = existingDto.getQuantity() != null ? existingDto.getQuantity() : 0;
                } catch (Exception parseEx) {
                    log.warn("Lỗi đọc JSON item cũ variantId {} cho guest {}, sẽ ghi đè.", variantId, guestId);
                }
            }

            int requestedNewQty = existingQty + request.getQuantity();
            if (requestedNewQty > 999) {
                throw new BadRequestException("Tổng số lượng cho sản phẩm này trong giỏ hàng không được vượt quá 999");
            }

            CatalogVariantValidationDto catalogDto = catalogServiceClient.getVariantDetails(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Biến thể sản phẩm (variantId=" + variantId + ") không tồn tại hoặc đã ngừng kinh doanh"));

            if (!catalogDto.isActive()) {
                throw new BadRequestException("Sản phẩm hoặc gian hàng hiện không còn hoạt động để thêm vào giỏ");
            }

            int availableStock = catalogDto.getAvailableStock() != null ? catalogDto.getAvailableStock() : 0;
            if (availableStock <= 0) {
                throw new BadRequestException("Biến thể sản phẩm này hiện đã hết hàng trong kho");
            }

            if (requestedNewQty > availableStock) {
                throw new BadRequestException(String.format(
                        "Số lượng yêu cầu (%d) vượt quá tồn kho khả dụng hiện tại (%d). Vui lòng giảm số lượng.",
                        requestedNewQty, availableStock
                ));
            }

            BigDecimal price = catalogDto.getPrice();
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(requestedNewQty));

            CartItemRedisDto updatedDto = CartItemRedisDto.builder()
                    .variantId(catalogDto.getVariantId())
                    .productId(catalogDto.getProductId())
                    .shopId(catalogDto.getShopId())
                    .shopName(catalogDto.getShopName() != null ? catalogDto.getShopName() : "Shop #" + catalogDto.getShopId())
                    .productName(catalogDto.getProductName())
                    .variantName(catalogDto.getVariantName())
                    .sku(catalogDto.getSku())
                    .imageUrl(catalogDto.getImageUrl())
                    .price(price)
                    .quantity(requestedNewQty)
                    .availableStock(availableStock)
                    .cartType(CartType.GUEST)
                    .ownerId(guestId)
                    .selected(true)
                    .itemSubtotal(itemSubtotal)
                    .updatedAt(LocalDateTime.now())
                    .build();

            stringRedisTemplate.opsForHash().put(cartKey, String.valueOf(variantId), objectMapper.writeValueAsString(updatedDto));
            cartRedisKeyManager.refreshTtl(cartKey, CartType.GUEST);

            log.info("Thêm thành công variantId {} (số lượng: {}) vào Guest Cart {}", variantId, requestedNewQty, guestId);
            return getGuestCart(guestId);

        } catch (BadRequestException | ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi thêm sản phẩm vào Guest Cart {}: {}", guestId, ex.getMessage(), ex);
            throw new CartOperationException("Thêm sản phẩm vào giỏ hàng khách thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public CartResponse updateGuestCartItemQuantity(String guestId, Long variantId, UpdateCartItemRequest request) {
        validateGuestId(guestId);
        if (variantId == null || variantId <= 0) {
            throw new BadRequestException("variantId không hợp lệ");
        }
        if (request == null || request.getQuantity() == null) {
            throw new BadRequestException("Thông tin cập nhật số lượng không được để trống");
        }
        if (request.getQuantity() < 0) {
            throw new BadRequestException("Số lượng không thể là số âm");
        }
        if (request.getQuantity() > 999) {
            throw new BadRequestException("Số lượng không được vượt quá 999");
        }

        if (request.getQuantity() == 0) {
            return removeGuestCartItem(guestId, variantId);
        }

        String cartKey = cartRedisKeyManager.getGuestCartKey(guestId);
        String lockKey = cartRedisKeyManager.getGuestLockKey(guestId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng khách đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            String existingJson = (String) stringRedisTemplate.opsForHash().get(cartKey, String.valueOf(variantId));
            if (existingJson == null) {
                throw new ResourceNotFoundException("Sản phẩm (variantId=" + variantId + ") không có trong giỏ hàng");
            }

            CartItemRedisDto currentDto = objectMapper.readValue(existingJson, CartItemRedisDto.class);

            CatalogVariantValidationDto catalogDto = catalogServiceClient.getVariantDetails(variantId)
                    .orElse(null);

            int availableStock = (catalogDto != null && catalogDto.getAvailableStock() != null)
                    ? catalogDto.getAvailableStock()
                    : (currentDto.getAvailableStock() != null ? currentDto.getAvailableStock() : 999);

            if (request.getQuantity() > availableStock) {
                throw new BadRequestException(String.format(
                        "Số lượng cập nhật (%d) vượt quá tồn kho khả dụng hiện tại (%d)",
                        request.getQuantity(), availableStock
                ));
            }

            BigDecimal price = (catalogDto != null && catalogDto.getPrice() != null) ? catalogDto.getPrice() : currentDto.getPrice();
            currentDto.setQuantity(request.getQuantity());
            currentDto.setAvailableStock(availableStock);
            currentDto.setPrice(price);
            currentDto.setItemSubtotal(price.multiply(BigDecimal.valueOf(request.getQuantity())));
            currentDto.setUpdatedAt(LocalDateTime.now());

            stringRedisTemplate.opsForHash().put(cartKey, String.valueOf(variantId), objectMapper.writeValueAsString(currentDto));
            cartRedisKeyManager.refreshTtl(cartKey, CartType.GUEST);

            log.info("Cập nhật số lượng variantId {} thành {} cho Guest Cart {}", variantId, request.getQuantity(), guestId);
            return getGuestCart(guestId);

        } catch (BadRequestException | ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi cập nhật số lượng cho guest {}: {}", guestId, ex.getMessage(), ex);
            throw new CartOperationException("Cập nhật giỏ hàng khách thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public CartResponse removeGuestCartItem(String guestId, Long variantId) {
        validateGuestId(guestId);
        if (variantId == null || variantId <= 0) {
            throw new BadRequestException("variantId không hợp lệ");
        }

        String cartKey = cartRedisKeyManager.getGuestCartKey(guestId);
        String lockKey = cartRedisKeyManager.getGuestLockKey(guestId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng khách đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            Long deleted = stringRedisTemplate.opsForHash().delete(cartKey, String.valueOf(variantId));
            if (deleted == null || deleted == 0) {
                throw new ResourceNotFoundException("Mặt hàng (variantId=" + variantId + ") không có trong giỏ hàng");
            }
            log.info("Đã xóa variantId {} khỏi Guest Cart {} (kết quả: {})", variantId, guestId, deleted);

            Long remaining = stringRedisTemplate.opsForHash().size(cartKey);
            if (remaining != null && remaining == 0) {
                stringRedisTemplate.delete(cartKey);
            } else {
                cartRedisKeyManager.refreshTtl(cartKey, CartType.GUEST);
            }

            return getGuestCart(guestId);
        } catch (BadRequestException | ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi xóa mặt hàng {} khỏi giỏ cho guest {}: {}", variantId, guestId, ex.getMessage(), ex);
            throw new CartOperationException("Xóa mặt hàng khỏi giỏ hàng khách thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public void clearGuestCart(String guestId) {
        validateGuestId(guestId);
        String cartKey = cartRedisKeyManager.getGuestCartKey(guestId);
        String lockKey = cartRedisKeyManager.getGuestLockKey(guestId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng khách đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            stringRedisTemplate.delete(cartKey);
            log.info("Đã làm trống toàn bộ Guest Cart {}", guestId);
        } catch (Exception ex) {
            log.error("Lỗi khi làm trống giỏ hàng cho guest {}: {}", guestId, ex.getMessage(), ex);
            throw new CartOperationException("Làm trống giỏ hàng khách thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    // ==========================================
    // CART METADATA & REFRESH TTL
    // ==========================================

    @Override
    public CartMetadataDto getCartMetadata(CartType cartType, String ownerId) {
        if (cartType == null) {
            cartType = CartType.USER;
        }

        String cartKey;
        if (cartType == CartType.GUEST) {
            validateGuestId(ownerId);
            cartKey = cartRedisKeyManager.getGuestCartKey(ownerId);
        } else {
            Long userId = Long.parseLong(ownerId);
            validateUserId(userId);
            cartKey = cartRedisKeyManager.resolveUserCartKey(userId);
        }

        Boolean exists = stringRedisTemplate.hasKey(cartKey);
        Long ttlSeconds = cartRedisKeyManager.getTtlSeconds(cartKey);
        Long size = stringRedisTemplate.opsForHash().size(cartKey);

        LocalDateTime expiresAt = (ttlSeconds != null && ttlSeconds > 0)
                ? LocalDateTime.now().plusSeconds(ttlSeconds)
                : null;

        return CartMetadataDto.builder()
                .cartKey(cartKey)
                .cartType(cartType)
                .ownerId(ownerId)
                .totalItems(size != null ? size.intValue() : 0)
                .ttlSeconds(ttlSeconds)
                .expiresAt(expiresAt)
                .exists(Boolean.TRUE.equals(exists))
                .build();
    }

    // ==========================================
    // MERGE & CHECKOUT HELPERS
    // ==========================================

    @Override
    public MergeCartResponse mergeGuestCartToUserCart(Long userId, String guestId, String requestId) {
        validateUserId(userId);
        validateGuestId(guestId);
        MergeCartRequest request = MergeCartRequest.builder()
                .guestId(guestId.trim())
                .requestId(requestId)
                .clearGuestCartAfterMerge(true)
                .items(Collections.emptyList())
                .build();
        return mergeCart(userId, request);
    }

    @Override
    public MergeCartResponse mergeCart(Long userId, MergeCartRequest request) {
        validateUserId(userId);
        if (request == null || !request.hasValidPayload()) {
            throw new BadRequestException("Yêu cầu hợp nhất giỏ hàng không hợp lệ: cần cung cấp guestId hoặc danh sách items");
        }

        String requestId = request.getRequestId();
        String idempotencyKey = (requestId != null && !requestId.isBlank())
                ? IDEMPOTENCY_KEY_PREFIX + userId + ":" + requestId.trim()
                : null;

        // 1. Kiểm tra Idempotency trước khi lấy lock để tối ưu hiệu năng
        if (idempotencyKey != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(idempotencyKey))) {
            log.info("Phát hiện yêu cầu hợp nhất giỏ hàng trùng lặp (requestId: {}) cho user {}. Trả về giỏ hàng hiện tại.",
                    requestId, userId);
            return MergeCartResponse.builder()
                    .cart(getCart(userId))
                    .warnings(Collections.singletonList("Yêu cầu hợp nhất đã được xử lý trước đó (Idempotent)."))
                    .mergedItemsCount(0)
                    .duplicateRequest(true)
                    .guestId(request.getGuestId())
                    .duplicateSkuCount(0)
                    .stockAdjustedCount(0)
                    .build();
        }

        // 2. Thu thập danh sách mặt hàng từ các nguồn (Redis Guest Cart và Request Items)
        List<GuestCartItemDto> combinedGuestItems = new ArrayList<>();
        String guestId = (request.getGuestId() != null && !request.getGuestId().isBlank()) ? request.getGuestId().trim() : null;
        String guestCartKey = (guestId != null) ? cartRedisKeyManager.getGuestCartKey(guestId) : null;

        if (guestCartKey != null) {
            try {
                Map<Object, Object> rawGuestEntries = stringRedisTemplate.opsForHash().entries(guestCartKey);
                if (rawGuestEntries != null && !rawGuestEntries.isEmpty()) {
                    for (Map.Entry<Object, Object> entry : rawGuestEntries.entrySet()) {
                        try {
                            CartItemRedisDto dto = objectMapper.readValue(String.valueOf(entry.getValue()), CartItemRedisDto.class);
                            if (dto.getVariantId() != null && dto.getQuantity() != null && dto.getQuantity() > 0) {
                                combinedGuestItems.add(GuestCartItemDto.builder()
                                        .variantId(dto.getVariantId())
                                        .quantity(dto.getQuantity())
                                        .build());
                            }
                        } catch (Exception parseEx) {
                            log.warn("Lỗi đọc item từ Redis Guest Cart {} tại field {}: {}", guestId, entry.getKey(), parseEx.getMessage());
                        }
                    }
                }
            } catch (Exception ex) {
                log.error("Lỗi khi đọc Redis Guest Cart cho guest {}: {}", guestId, ex.getMessage(), ex);
            }
        }

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            combinedGuestItems.addAll(request.getItems());
        }

        if (combinedGuestItems.isEmpty()) {
            log.info("Không có mặt hàng nào cần hợp nhất cho user {} (guestId: {})", userId, guestId);
            return MergeCartResponse.builder()
                    .cart(getCart(userId))
                    .warnings(Collections.emptyList())
                    .mergedItemsCount(0)
                    .duplicateRequest(false)
                    .guestId(guestId)
                    .clearedGuestCart(false)
                    .duplicateSkuCount(0)
                    .stockAdjustedCount(0)
                    .build();
        }

        // 3. Gộp các mặt hàng trùng SKU/variantId trong nguồn dữ liệu khách vãng lai
        Map<Long, Integer> aggregatedGuestItems = new LinkedHashMap<>();
        for (GuestCartItemDto guestItem : combinedGuestItems) {
            if (guestItem == null || guestItem.getVariantId() == null || guestItem.getQuantity() == null || guestItem.getQuantity() <= 0) {
                continue;
            }
            aggregatedGuestItems.merge(guestItem.getVariantId(), guestItem.getQuantity(), Integer::sum);
        }

        String cartKey = cartRedisKeyManager.resolveUserCartKey(userId);
        String lockKey = cartRedisKeyManager.getUserLockKey(userId);
        String guestLockKey = (guestId != null) ? cartRedisKeyManager.getGuestLockKey(guestId) : null;

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang bận xử lý hợp nhất, vui lòng thử lại sau giây lát");
        }

        String guestLockToken = null;
        if (guestLockKey != null) {
            guestLockToken = redisDistributedLock.acquireLock(guestLockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        }

        try {
            // Kiểm tra lại Idempotency sau khi có lock
            if (idempotencyKey != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(idempotencyKey))) {
                return MergeCartResponse.builder()
                        .cart(getCart(userId))
                        .warnings(Collections.singletonList("Yêu cầu hợp nhất đã được xử lý trước đó."))
                        .mergedItemsCount(0)
                        .duplicateRequest(true)
                        .guestId(guestId)
                        .duplicateSkuCount(0)
                        .stockAdjustedCount(0)
                        .build();
            }

            // 4. Đọc toàn bộ giỏ hàng User hiện có trong Redis Hash
            Map<Object, Object> rawEntries = stringRedisTemplate.opsForHash().entries(cartKey);
            Map<Long, CartItemRedisDto> currentCartMap = new HashMap<>();

            if (rawEntries != null && !rawEntries.isEmpty()) {
                for (Map.Entry<Object, Object> entry : rawEntries.entrySet()) {
                    try {
                        CartItemRedisDto item = objectMapper.readValue(String.valueOf(entry.getValue()), CartItemRedisDto.class);
                        currentCartMap.put(item.getVariantId(), item);
                    } catch (Exception ex) {
                        log.warn("Không thể parse item trong cart của user {} tại field {}", userId, entry.getKey());
                    }
                }
            }

            List<String> warnings = new ArrayList<>();
            int mergedCount = 0;
            int duplicateSkuCount = 0;
            int stockAdjustedCount = 0;

            // 5. Xử lý kiểm tra trùng SKU và giới hạn tồn kho cho từng biến thể
            for (Map.Entry<Long, Integer> entry : aggregatedGuestItems.entrySet()) {
                Long variantId = entry.getKey();
                int guestQty = entry.getValue();

                Optional<CatalogVariantValidationDto> optCatalog = catalogServiceClient.getVariantDetails(variantId);
                if (optCatalog.isEmpty() || !optCatalog.get().isActive()) {
                    warnings.add("Sản phẩm (Mã biến thể #" + variantId + ") không còn khả dụng hoặc đã ngừng kinh doanh, đã được bỏ qua.");
                    continue;
                }

                CatalogVariantValidationDto catalogDto = optCatalog.get();
                int availableStock = catalogDto.getAvailableStock() != null ? catalogDto.getAvailableStock() : 0;
                if (availableStock <= 0) {
                    warnings.add("Sản phẩm '" + catalogDto.getProductName() + "' hiện đã hết hàng, không thể thêm vào giỏ.");
                    continue;
                }

                boolean isDuplicateSku = currentCartMap.containsKey(variantId);
                int currentQty = 0;
                if (isDuplicateSku) {
                    CartItemRedisDto existing = currentCartMap.get(variantId);
                    currentQty = existing.getQuantity() != null ? existing.getQuantity() : 0;
                    duplicateSkuCount++;
                    log.info("Phát hiện trùng SKU [{}] (variantId: {}) trong User Cart {}. Số lượng hiện có: {}, số lượng thêm từ khách: {}",
                            catalogDto.getSku(), variantId, userId, currentQty, guestQty);
                }

                int finalQty = currentQty + guestQty;

                if (finalQty > availableStock) {
                    stockAdjustedCount++;
                    finalQty = availableStock;
                    warnings.add(String.format(
                            "Sản phẩm '%s' chỉ còn %d sản phẩm khả dụng trong kho. Số lượng trong giỏ hàng đã được điều chỉnh về %d.",
                            catalogDto.getProductName(), availableStock, finalQty
                    ));
                }

                if (finalQty > 999) {
                    finalQty = 999;
                    warnings.add(String.format("Sản phẩm '%s' đạt số lượng tối đa cho phép (999).", catalogDto.getProductName()));
                }

                if (finalQty <= 0) {
                    continue;
                }

                BigDecimal price = catalogDto.getPrice();
                CartItemRedisDto mergedDto = CartItemRedisDto.builder()
                        .variantId(catalogDto.getVariantId())
                        .productId(catalogDto.getProductId())
                        .shopId(catalogDto.getShopId())
                        .shopName(catalogDto.getShopName() != null ? catalogDto.getShopName() : "Shop #" + catalogDto.getShopId())
                        .productName(catalogDto.getProductName())
                        .variantName(catalogDto.getVariantName())
                        .sku(catalogDto.getSku())
                        .imageUrl(catalogDto.getImageUrl())
                        .price(price)
                        .quantity(finalQty)
                        .availableStock(availableStock)
                        .cartType(CartType.USER)
                        .ownerId(String.valueOf(userId))
                        .selected(true)
                        .itemSubtotal(price.multiply(BigDecimal.valueOf(finalQty)))
                        .updatedAt(LocalDateTime.now())
                        .build();

                currentCartMap.put(variantId, mergedDto);
                mergedCount++;
            }

            // 6. Ghi nhận toàn bộ dữ liệu đã hợp nhất vào Redis Hash một cách an toàn
            Map<String, String> writesToRedis = new HashMap<>();
            for (CartItemRedisDto item : currentCartMap.values()) {
                writesToRedis.put(String.valueOf(item.getVariantId()), objectMapper.writeValueAsString(item));
            }

            if (!writesToRedis.isEmpty()) {
                stringRedisTemplate.opsForHash().putAll(cartKey, writesToRedis);
                cartRedisKeyManager.refreshTtl(cartKey, CartType.USER);
            }

            // 7. Tự động dọn dẹp Redis Guest Cart nếu được yêu cầu
            boolean clearedGuest = false;
            if (guestCartKey != null && request.isClearGuestCartAfterMerge()) {
                Boolean deleted = stringRedisTemplate.delete(guestCartKey);
                clearedGuest = Boolean.TRUE.equals(deleted);
                log.info("Đã xóa dọn dẹp Redis Guest Cart [{}] sau khi hợp nhất vào User Cart {} (kết quả: {})",
                        guestCartKey, userId, clearedGuest);
            }

            // 8. Ghi nhận Idempotency Token thành công
            if (idempotencyKey != null) {
                stringRedisTemplate.opsForValue().set(idempotencyKey, "PROCESSED", IDEMPOTENCY_TTL);
            }

            log.info("Hợp nhất thành công {} mặt hàng (trùng SKU: {}, điều chỉnh tồn kho: {}) từ Guest Cart vào User Cart cho user {}",
                    mergedCount, duplicateSkuCount, stockAdjustedCount, userId);

            // 9. Đọc lại giỏ hàng đầy đủ đã được gom nhóm để trả về
            CartResponse updatedCart = getCart(userId);

            return MergeCartResponse.builder()
                    .cart(updatedCart)
                    .warnings(warnings)
                    .mergedItemsCount(mergedCount)
                    .duplicateRequest(false)
                    .guestId(guestId)
                    .clearedGuestCart(clearedGuest)
                    .duplicateSkuCount(duplicateSkuCount)
                    .stockAdjustedCount(stockAdjustedCount)
                    .build();

        } catch (CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi hợp nhất giỏ hàng cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Hợp nhất giỏ hàng thất bại: " + ex.getMessage(), ex);
        } finally {
            if (guestLockToken != null && guestLockKey != null) {
                redisDistributedLock.releaseLock(guestLockKey, guestLockToken);
            }
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public void removeCartItems(Long userId, List<Long> variantIds) {
        validateUserId(userId);
        if (variantIds == null || variantIds.isEmpty()) {
            return;
        }

        String cartKey = cartRedisKeyManager.resolveUserCartKey(userId);
        String lockKey = cartRedisKeyManager.getUserLockKey(userId);
        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);

        if (lockToken == null) {
            log.warn("Không thể lấy lock khi xóa items đã checkout cho user {}", userId);
            throw new CartLockException("Hệ thống đang bận cập nhật giỏ hàng, vui lòng thử lại sau.");
        }

        try {
            Object[] fieldsToDelete = variantIds.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toArray();

            if (fieldsToDelete.length > 0) {
                stringRedisTemplate.opsForHash().delete(cartKey, fieldsToDelete);
                log.info("Đã xóa {} mặt hàng đã checkout khỏi giỏ hàng của user {}", fieldsToDelete.length, userId);
                cartRedisKeyManager.refreshTtl(cartKey, CartType.USER);
            }
        } catch (Exception ex) {
            log.error("Lỗi khi xóa các mặt hàng đã checkout khỏi Redis cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Lỗi hệ thống khi dọn dẹp mặt hàng đã đặt khỏi giỏ", ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Định danh người dùng (userId) không hợp lệ");
        }
    }

    private void validateGuestId(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            throw new BadRequestException("Định danh khách vãng lai (guestId) không hợp lệ");
        }
    }

    private void validateAddToCartRequest(AddToCartRequest request) {
        if (request == null || request.getVariantId() == null || request.getQuantity() == null) {
            throw new BadRequestException("Yêu cầu thêm vào giỏ hàng không đầy đủ thông tin");
        }
        if (request.getQuantity() <= 0) {
            throw new BadRequestException("Số lượng thêm vào giỏ hàng phải lớn hơn 0");
        }
        if (request.getQuantity() > 999) {
            throw new BadRequestException("Số lượng sản phẩm vượt quá giới hạn tối đa cho phép (999)");
        }
    }

    private CartItemResponse toItemResponse(CartItemRedisDto dto) {
        BigDecimal price = dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO;
        int qty = dto.getQuantity() != null ? dto.getQuantity() : 0;
        int stock = dto.getAvailableStock() != null ? dto.getAvailableStock() : 0;

        BigDecimal subtotal = (dto.getItemSubtotal() != null)
                ? dto.getItemSubtotal()
                : price.multiply(BigDecimal.valueOf(qty));

        boolean outOfStock = stock <= 0;
        boolean exceedsStock = qty > stock;

        return CartItemResponse.builder()
                .variantId(dto.getVariantId())
                .productId(dto.getProductId())
                .shopId(dto.getShopId())
                .shopName(dto.getShopName())
                .productName(dto.getProductName())
                .variantName(dto.getVariantName())
                .sku(dto.getSku())
                .imageUrl(dto.getImageUrl())
                .price(price)
                .quantity(qty)
                .availableStock(stock)
                .itemSubtotal(subtotal)
                .outOfStock(outOfStock)
                .quantityExceedsStock(exceedsStock)
                .build();
    }

    private CartResponse groupItemsByShop(Long userId, List<CartItemResponse> items) {
        CartResponse.CartResponseBuilder builder = CartResponse.builder()
                .userId(userId)
                .cartType(CartType.USER);

        return populateShopGroups(builder, items);
    }

    private CartResponse groupItemsByShopForGuest(String guestId, List<CartItemResponse> items) {
        CartResponse.CartResponseBuilder builder = CartResponse.builder()
                .guestId(guestId)
                .cartType(CartType.GUEST);

        return populateShopGroups(builder, items);
    }

    private CartResponse populateShopGroups(CartResponse.CartResponseBuilder builder, List<CartItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return builder
                    .shops(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .totalQuantity(0)
                    .build();
        }

        Map<Long, List<CartItemResponse>> itemsByShop = items.stream()
                .collect(Collectors.groupingBy(CartItemResponse::getShopId, LinkedHashMap::new, Collectors.toList()));

        List<CartShopGroupResponse> shopGroups = new ArrayList<>();
        BigDecimal totalCartAmount = BigDecimal.ZERO;
        int totalCartQuantity = 0;

        for (Map.Entry<Long, List<CartItemResponse>> entry : itemsByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<CartItemResponse> shopItems = entry.getValue();

            BigDecimal shopSubtotal = shopItems.stream()
                    .map(CartItemResponse::getItemSubtotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int shopItemCount = shopItems.stream()
                    .mapToInt(CartItemResponse::getQuantity)
                    .sum();

            String shopName = shopItems.stream()
                    .map(CartItemResponse::getShopName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .orElse("Shop #" + shopId);

            CartShopGroupResponse group = CartShopGroupResponse.builder()
                    .shopId(shopId)
                    .shopName(shopName)
                    .items(shopItems)
                    .shopSubtotal(shopSubtotal)
                    .shopTotalItems(shopItemCount)
                    .build();

            shopGroups.add(group);
            totalCartAmount = totalCartAmount.add(shopSubtotal);
            totalCartQuantity += shopItemCount;
        }

        return builder
                .shops(shopGroups)
                .totalAmount(totalCartAmount)
                .totalItems(items.size())
                .totalQuantity(totalCartQuantity)
                .build();
    }
}
