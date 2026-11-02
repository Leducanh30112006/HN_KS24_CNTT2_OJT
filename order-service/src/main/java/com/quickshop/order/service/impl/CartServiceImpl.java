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

    private static final String CART_KEY_PREFIX = "cart:";
    private static final String LOCK_KEY_PREFIX = "lock:cart:";
    private static final String IDEMPOTENCY_KEY_PREFIX = "cart:merge:idempotency:";
    private static final Duration CART_TTL = Duration.ofDays(30);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(15);

    // Thông số cấu hình Distributed Lock: timeout 3000ms, retry 5 lần, mỗi lần cách 50ms
    private static final long LOCK_TIMEOUT_MS = 3000L;
    private static final int LOCK_MAX_RETRIES = 5;
    private static final long LOCK_RETRY_INTERVAL_MS = 50L;

    @Override
    public CartResponse getCart(Long userId) {
        validateUserId(userId);
        String cartKey = buildCartKey(userId);

        Map<Object, Object> rawEntries;
        try {
            rawEntries = stringRedisTemplate.opsForHash().entries(cartKey);
        } catch (Exception ex) {
            log.error("Lỗi khi đọc giỏ hàng từ Redis cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Lỗi hệ thống khi tải giỏ hàng từ bộ nhớ đệm", ex);
        }

        if (rawEntries == null || rawEntries.isEmpty()) {
            return CartResponse.builder()
                    .userId(userId)
                    .shops(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .totalQuantity(0)
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

        return groupItemsByShop(userId, validItems);
    }

    @Override
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        validateUserId(userId);
        if (request == null || request.getVariantId() == null || request.getQuantity() == null) {
            throw new BadRequestException("Yêu cầu thêm vào giỏ hàng không đầy đủ thông tin");
        }
        if (request.getQuantity() <= 0) {
            throw new BadRequestException("Số lượng thêm vào giỏ hàng phải lớn hơn 0");
        }
        if (request.getQuantity() > 999) {
            throw new BadRequestException("Số lượng sản phẩm vượt quá giới hạn tối đa cho phép (999)");
        }

        Long variantId = request.getVariantId();
        String cartKey = buildCartKey(userId);
        String lockKey = buildLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang được cập nhật bởi một yêu cầu khác, vui lòng thử lại sau giây lát");
        }

        try {
            // 1. Kiểm tra tồn tại trong giỏ hiện tại
            Object existingVal = stringRedisTemplate.opsForHash().get(cartKey, String.valueOf(variantId));
            int currentQty = 0;
            if (existingVal != null) {
                try {
                    CartItemRedisDto currentDto = objectMapper.readValue(String.valueOf(existingVal), CartItemRedisDto.class);
                    currentQty = currentDto.getQuantity();
                } catch (Exception ex) {
                    log.warn("Lỗi đọc item cũ {} của user {}, sẽ ghi đè.", variantId, userId);
                }
            }

            int finalQuantity = currentQty + request.getQuantity();
            if (finalQuantity > 999) {
                throw new BadRequestException("Tổng số lượng cho mặt hàng này trong giỏ (" + finalQuantity + ") vượt quá giới hạn 999");
            }

            // 2. Xác thực và lấy snapshot từ Catalog Service
            CatalogVariantValidationDto catalogVariant = catalogServiceClient.getVariantDetails(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Biến thể sản phẩm không tồn tại với ID: " + variantId));

            if (!catalogVariant.isActive()) {
                throw new BadRequestException("Sản phẩm hoặc biến thể này hiện đã ngừng kinh doanh");
            }

            int availableStock = catalogVariant.getAvailableStock() != null ? catalogVariant.getAvailableStock() : 0;
            if (finalQuantity > availableStock) {
                throw new BadRequestException(String.format(
                        "Số lượng yêu cầu (%d) vượt quá tồn kho khả dụng (%d) của sản phẩm '%s'",
                        finalQuantity, availableStock, catalogVariant.getProductName()
                ));
            }

            // 3. Tạo snapshot DTO để lưu vào Redis
            CartItemRedisDto newDto = CartItemRedisDto.builder()
                    .variantId(catalogVariant.getVariantId())
                    .productId(catalogVariant.getProductId())
                    .shopId(catalogVariant.getShopId())
                    .shopName(catalogVariant.getShopName() != null ? catalogVariant.getShopName() : "Shop #" + catalogVariant.getShopId())
                    .productName(catalogVariant.getProductName())
                    .variantName(catalogVariant.getVariantName())
                    .sku(catalogVariant.getSku())
                    .imageUrl(catalogVariant.getImageUrl())
                    .price(catalogVariant.getPrice())
                    .quantity(finalQuantity)
                    .availableStock(availableStock)
                    .updatedAt(LocalDateTime.now())
                    .build();

            String jsonPayload = objectMapper.writeValueAsString(newDto);
            stringRedisTemplate.opsForHash().put(cartKey, String.valueOf(variantId), jsonPayload);
            stringRedisTemplate.expire(cartKey, CART_TTL);

            log.info("Thêm thành công variantId {} (qty: {}) vào giỏ hàng user {}", variantId, finalQuantity, userId);

            return getCart(userId);

        } catch (BadRequestException | ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi thêm sản phẩm vào giỏ hàng cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Thao tác thêm vào giỏ hàng thất bại: " + ex.getMessage(), ex);
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
        if (request == null || request.getQuantity() == null || request.getQuantity() < 0) {
            throw new BadRequestException("Số lượng cập nhật không hợp lệ (phải >= 0)");
        }

        // Nếu số lượng = 0 thì tương đương xóa khỏi giỏ
        if (request.getQuantity() == 0) {
            return removeCartItem(userId, variantId);
        }

        String cartKey = buildCartKey(userId);
        String lockKey = buildLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            Object existingVal = stringRedisTemplate.opsForHash().get(cartKey, String.valueOf(variantId));
            if (existingVal == null) {
                throw new ResourceNotFoundException("Mặt hàng không tồn tại trong giỏ hàng");
            }

            CartItemRedisDto currentDto = objectMapper.readValue(String.valueOf(existingVal), CartItemRedisDto.class);

            // Kiểm tra tồn kho mới nhất với Catalog Service
            CatalogVariantValidationDto catalogVariant = catalogServiceClient.getVariantDetails(variantId)
                    .orElse(null);

            if (catalogVariant != null) {
                if (!catalogVariant.isActive()) {
                    throw new BadRequestException("Sản phẩm này hiện đã ngừng kinh doanh");
                }
                int availableStock = catalogVariant.getAvailableStock() != null ? catalogVariant.getAvailableStock() : 0;
                if (request.getQuantity() > availableStock) {
                    throw new BadRequestException(String.format(
                            "Số lượng cập nhật (%d) vượt quá tồn kho khả dụng (%d)",
                            request.getQuantity(), availableStock
                    ));
                }
                // Cập nhật lại snapshot mới nhất
                currentDto.setPrice(catalogVariant.getPrice());
                currentDto.setAvailableStock(availableStock);
                currentDto.setProductName(catalogVariant.getProductName());
                currentDto.setVariantName(catalogVariant.getVariantName());
            }

            currentDto.setQuantity(request.getQuantity());
            currentDto.setUpdatedAt(LocalDateTime.now());

            String jsonPayload = objectMapper.writeValueAsString(currentDto);
            stringRedisTemplate.opsForHash().put(cartKey, String.valueOf(variantId), jsonPayload);
            stringRedisTemplate.expire(cartKey, CART_TTL);

            log.info("Cập nhật số lượng variantId {} thành {} cho user {}", variantId, request.getQuantity(), userId);

            return getCart(userId);

        } catch (BadRequestException | ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi cập nhật số lượng item {} cho user {}: {}", variantId, userId, ex.getMessage(), ex);
            throw new CartOperationException("Cập nhật số lượng thất bại: " + ex.getMessage(), ex);
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

        String cartKey = buildCartKey(userId);
        String lockKey = buildLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            Long deleted = stringRedisTemplate.opsForHash().delete(cartKey, String.valueOf(variantId));
            if (deleted == null || deleted == 0) {
                throw new ResourceNotFoundException("Mặt hàng không tồn tại trong giỏ hàng");
            }

            log.info("Đã xóa variantId {} khỏi giỏ hàng user {}", variantId, userId);

            // Kiểm tra nếu giỏ đã hết hàng thì có thể xóa key
            Long remaining = stringRedisTemplate.opsForHash().size(cartKey);
            if (remaining != null && remaining == 0) {
                stringRedisTemplate.delete(cartKey);
            } else {
                stringRedisTemplate.expire(cartKey, CART_TTL);
            }

            return getCart(userId);

        } catch (ResourceNotFoundException | CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi xóa item {} cho user {}: {}", variantId, userId, ex.getMessage(), ex);
            throw new CartOperationException("Xóa mặt hàng khỏi giỏ hàng thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public void clearCart(Long userId) {
        validateUserId(userId);
        String cartKey = buildCartKey(userId);
        String lockKey = buildLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang bận xử lý, vui lòng thử lại sau giây lát");
        }

        try {
            stringRedisTemplate.delete(cartKey);
            log.info("Đã làm trống toàn bộ giỏ hàng của user {}", userId);
        } catch (Exception ex) {
            log.error("Lỗi khi làm trống giỏ hàng cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Làm trống giỏ hàng thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Định danh người dùng (userId) không hợp lệ");
        }
    }

    private String buildCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private String buildLockKey(Long userId) {
        return LOCK_KEY_PREFIX + userId;
    }

    private CartItemResponse toItemResponse(CartItemRedisDto dto) {
        BigDecimal price = dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO;
        int qty = dto.getQuantity() != null ? dto.getQuantity() : 0;
        int stock = dto.getAvailableStock() != null ? dto.getAvailableStock() : 0;

        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));
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

    /**
     * Gom nhóm các mặt hàng theo shopId, tính toán tổng tiền từng shop và toàn giỏ.
     */
    private CartResponse groupItemsByShop(Long userId, List<CartItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return CartResponse.builder()
                    .userId(userId)
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

            // Trích xuất tên shop từ item đầu tiên nếu có
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

        return CartResponse.builder()
                .userId(userId)
                .shops(shopGroups)
                .totalAmount(totalCartAmount)
                .totalItems(items.size())
                .totalQuantity(totalCartQuantity)
                .build();
    }

    @Override
    public MergeCartResponse mergeCart(Long userId, MergeCartRequest request) {
        validateUserId(userId);
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return MergeCartResponse.builder()
                    .cart(getCart(userId))
                    .warnings(Collections.emptyList())
                    .mergedItemsCount(0)
                    .duplicateRequest(false)
                    .build();
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
                    .build();
        }

        String cartKey = buildCartKey(userId);
        String lockKey = buildLockKey(userId);

        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            throw new CartLockException("Giỏ hàng đang được cập nhật bởi một thao tác khác, vui lòng thử lại sau giây lát");
        }

        try {
            // 2. Double-checked locking cho Idempotency bên trong lock
            if (idempotencyKey != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(idempotencyKey))) {
                log.info("Yêu cầu trùng lặp phát hiện bên trong lock cho user {}", userId);
                return MergeCartResponse.builder()
                        .cart(getCart(userId))
                        .warnings(Collections.singletonList("Yêu cầu hợp nhất đã được xử lý trước đó."))
                        .mergedItemsCount(0)
                        .duplicateRequest(true)
                        .build();
            }

            // 3. Đọc toàn bộ giỏ hàng hiện tại của user từ Redis
            Map<Object, Object> rawEntries = stringRedisTemplate.opsForHash().entries(cartKey);
            Map<Long, CartItemRedisDto> currentCartMap = new HashMap<>();
            if (rawEntries != null) {
                for (Map.Entry<Object, Object> entry : rawEntries.entrySet()) {
                    try {
                        CartItemRedisDto dto = objectMapper.readValue(String.valueOf(entry.getValue()), CartItemRedisDto.class);
                        currentCartMap.put(dto.getVariantId(), dto);
                    } catch (Exception parseEx) {
                        log.warn("Bỏ qua entry hỏng {} của user {}", entry.getKey(), userId);
                    }
                }
            }

            List<String> warnings = new ArrayList<>();
            int mergedCount = 0;

            // 4. Duyệt qua từng mặt hàng trong Guest Cart và áp dụng thuật toán hợp nhất
            for (GuestCartItemDto guestItem : request.getItems()) {
                if (guestItem == null || guestItem.getVariantId() == null || guestItem.getQuantity() == null || guestItem.getQuantity() <= 0) {
                    continue;
                }

                Long variantId = guestItem.getVariantId();
                int guestQty = guestItem.getQuantity();

                // Xác minh với Catalog Service
                Optional<CatalogVariantValidationDto> catalogOpt = catalogServiceClient.getVariantDetails(variantId);
                if (catalogOpt.isEmpty()) {
                    warnings.add(String.format("Biến thể sản phẩm (ID: %d) không tồn tại và đã được bỏ qua.", variantId));
                    continue;
                }

                CatalogVariantValidationDto catalogDto = catalogOpt.get();
                if (!catalogDto.isActive()) {
                    warnings.add(String.format("Sản phẩm '%s' hiện đã ngừng kinh doanh và không thể hợp nhất vào giỏ.", catalogDto.getProductName()));
                    continue;
                }

                int availableStock = catalogDto.getAvailableStock() != null ? catalogDto.getAvailableStock() : 0;
                if (availableStock <= 0) {
                    warnings.add(String.format("Sản phẩm '%s' hiện đã hết hàng trong kho.", catalogDto.getProductName()));
                    continue;
                }

                CartItemRedisDto existingUserItem = currentCartMap.get(variantId);
                int currentQty = existingUserItem != null ? existingUserItem.getQuantity() : 0;
                int targetQty = currentQty + guestQty;
                int finalQty = targetQty;

                // Không để số lượng vượt quá tồn kho khả dụng
                if (targetQty > availableStock) {
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

                CartItemRedisDto mergedDto = CartItemRedisDto.builder()
                        .variantId(catalogDto.getVariantId())
                        .productId(catalogDto.getProductId())
                        .shopId(catalogDto.getShopId())
                        .shopName(catalogDto.getShopName() != null ? catalogDto.getShopName() : "Shop #" + catalogDto.getShopId())
                        .productName(catalogDto.getProductName())
                        .variantName(catalogDto.getVariantName())
                        .sku(catalogDto.getSku())
                        .imageUrl(catalogDto.getImageUrl())
                        .price(catalogDto.getPrice())
                        .quantity(finalQty)
                        .availableStock(availableStock)
                        .updatedAt(LocalDateTime.now())
                        .build();

                currentCartMap.put(variantId, mergedDto);
                mergedCount++;
            }

            // 5. Ghi nhận toàn bộ dữ liệu đã hợp nhất vào Redis Hash một cách an toàn
            Map<String, String> writesToRedis = new HashMap<>();
            for (CartItemRedisDto item : currentCartMap.values()) {
                writesToRedis.put(String.valueOf(item.getVariantId()), objectMapper.writeValueAsString(item));
            }

            if (!writesToRedis.isEmpty()) {
                stringRedisTemplate.opsForHash().putAll(cartKey, writesToRedis);
                stringRedisTemplate.expire(cartKey, CART_TTL);
            }

            // 6. Ghi nhận Idempotency Token thành công
            if (idempotencyKey != null) {
                stringRedisTemplate.opsForValue().set(idempotencyKey, "PROCESSED", IDEMPOTENCY_TTL);
            }

            log.info("Hợp nhất thành công {} mặt hàng từ Guest Cart vào User Cart cho user {}", mergedCount, userId);

            // 7. Đọc lại giỏ hàng đầy đủ đã được gom nhóm để trả về
            CartResponse updatedCart = getCart(userId);

            return MergeCartResponse.builder()
                    .cart(updatedCart)
                    .warnings(warnings)
                    .mergedItemsCount(mergedCount)
                    .duplicateRequest(false)
                    .build();

        } catch (CartLockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi hợp nhất giỏ hàng cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Hợp nhất giỏ hàng thất bại: " + ex.getMessage(), ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    public void removeCartItems(Long userId, List<Long> variantIds) {
        validateUserId(userId);
        if (variantIds == null || variantIds.isEmpty()) {
            return;
        }

        String cartKey = buildCartKey(userId);
        String lockKey = buildLockKey(userId);
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
            }
        } catch (Exception ex) {
            log.error("Lỗi khi xóa các mặt hàng đã checkout khỏi Redis cho user {}: {}", userId, ex.getMessage(), ex);
            throw new CartOperationException("Lỗi hệ thống khi dọn dẹp mặt hàng đã đặt khỏi giỏ", ex);
        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }
}

