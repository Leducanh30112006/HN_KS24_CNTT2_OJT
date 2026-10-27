package com.quickshop.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.order.dto.cart.AddToCartRequest;
import com.quickshop.order.dto.cart.CartResponse;
import com.quickshop.order.dto.cart.CartShopGroupResponse;
import com.quickshop.order.dto.cart.GuestCartItemDto;
import com.quickshop.order.dto.cart.MergeCartRequest;
import com.quickshop.order.dto.cart.MergeCartResponse;
import com.quickshop.order.dto.cart.UpdateCartItemRequest;
import com.quickshop.order.exception.GlobalExceptionHandler;
import com.quickshop.order.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CartController.class)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final Long USER_ID = 1001L;

    @Test
    @DisplayName("GET /api/cart có header X-User-Id -> Trả về 200 OK và giỏ hàng")
    void testGetCart_WithHeader_Success() throws Exception {
        CartResponse cartResponse = CartResponse.builder()
                .userId(USER_ID)
                .shops(Collections.emptyList())
                .totalAmount(BigDecimal.ZERO)
                .totalItems(0)
                .totalQuantity(0)
                .build();

        when(cartService.getCart(USER_ID)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart")
                        .header("X-User-Id", USER_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy giỏ hàng thành công"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID));

        verify(cartService).getCart(USER_ID);
    }

    @Test
    @DisplayName("GET /api/cart thiếu thông tin định danh userId -> Trả về 400 Bad Request")
    void testGetCart_MissingUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Yêu cầu thông tin định danh người dùng qua X-User-Id header hoặc param userId"));

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /api/cart/items hợp lệ -> Trả về 201 Created")
    void testAddToCart_ValidRequest_ReturnsCreated() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(201L)
                .quantity(2)
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .userId(USER_ID)
                .shops(Collections.singletonList(
                        CartShopGroupResponse.builder()
                                .shopId(10L)
                                .shopName("Shop A")
                                .shopSubtotal(BigDecimal.valueOf(200000))
                                .shopTotalItems(2)
                                .build()
                ))
                .totalAmount(BigDecimal.valueOf(200000))
                .totalItems(1)
                .totalQuantity(2)
                .build();

        when(cartService.addToCart(eq(USER_ID), any(AddToCartRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(200000));

        verify(cartService).addToCart(eq(USER_ID), any(AddToCartRequest.class));
    }

    @Test
    @DisplayName("POST /api/cart/items số lượng <= 0 -> Validation bắt lỗi trả về 400")
    void testAddToCart_InvalidQuantity_ReturnsBadRequest() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(201L)
                .quantity(0) // Không hợp lệ, min = 1
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.quantity").exists());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /api/cart/items thiếu variantId -> Validation bắt lỗi trả về 400")
    void testAddToCart_NullVariantId_ReturnsBadRequest() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(null)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.variantId").exists());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("PUT /api/cart/items/{variantId} hợp lệ -> Trả về 200 OK")
    void testUpdateCartItem_Success() throws Exception {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(5)
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .userId(USER_ID)
                .totalQuantity(5)
                .build();

        when(cartService.updateCartItemQuantity(eq(USER_ID), eq(201L), any(UpdateCartItemRequest.class)))
                .thenReturn(cartResponse);

        mockMvc.perform(put("/api/cart/items/201")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật số lượng thành công"));

        verify(cartService).updateCartItemQuantity(eq(USER_ID), eq(201L), any(UpdateCartItemRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{variantId} -> Trả về 200 OK")
    void testRemoveCartItem_Success() throws Exception {
        CartResponse cartResponse = CartResponse.builder()
                .userId(USER_ID)
                .totalQuantity(0)
                .build();

        when(cartService.removeCartItem(USER_ID, 201L)).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/items/201")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa sản phẩm khỏi giỏ hàng thành công"));

        verify(cartService).removeCartItem(USER_ID, 201L);
    }

    @Test
    @DisplayName("DELETE /api/cart -> Làm trống toàn bộ giỏ hàng, trả về 200 OK")
    void testClearCart_Success() throws Exception {
        doNothing().when(cartService).clearCart(USER_ID);

        mockMvc.perform(delete("/api/cart")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã làm trống giỏ hàng"));

        verify(cartService).clearCart(USER_ID);
    }

    @Test
    @DisplayName("POST /api/cart/merge hợp lệ -> Trả về 200 OK và giỏ hàng đã hợp nhất")
    void testMergeCart_Success() throws Exception {
        GuestCartItemDto item = GuestCartItemDto.builder()
                .variantId(201L)
                .quantity(2)
                .build();

        MergeCartRequest request = MergeCartRequest.builder()
                .requestId("req-client-123")
                .items(List.of(item))
                .build();

        CartResponse mergedCart = CartResponse.builder()
                .userId(USER_ID)
                .totalQuantity(2)
                .totalAmount(BigDecimal.valueOf(200000))
                .build();

        MergeCartResponse mergeResponse = MergeCartResponse.builder()
                .cart(mergedCart)
                .mergedItemsCount(1)
                .warnings(Collections.emptyList())
                .duplicateRequest(false)
                .build();

        when(cartService.mergeCart(eq(USER_ID), any(MergeCartRequest.class)))
                .thenReturn(mergeResponse);

        mockMvc.perform(post("/api/cart/merge")
                        .header("X-User-Id", USER_ID)
                        .header("X-Idempotency-Key", "req-client-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hợp nhất giỏ hàng thành công"))
                .andExpect(jsonPath("$.data.mergedItemsCount").value(1))
                .andExpect(jsonPath("$.data.duplicateRequest").value(false))
                .andExpect(jsonPath("$.data.cart.userId").value(USER_ID));

        verify(cartService).mergeCart(eq(USER_ID), any(MergeCartRequest.class));
    }

    @Test
    @DisplayName("POST /api/cart/merge danh sách rỗng -> Validation báo lỗi 400 Bad Request")
    void testMergeCart_EmptyItems_ReturnsBadRequest() throws Exception {
        MergeCartRequest request = MergeCartRequest.builder()
                .requestId("req-client-empty")
                .items(Collections.emptyList()) // Danh sách rỗng
                .build();

        mockMvc.perform(post("/api/cart/merge")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.items").exists());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /api/cart/merge thiếu định danh userId -> Báo lỗi 400 Bad Request")
    void testMergeCart_MissingUserId_ReturnsBadRequest() throws Exception {
        GuestCartItemDto item = GuestCartItemDto.builder()
                .variantId(201L)
                .quantity(1)
                .build();

        MergeCartRequest request = MergeCartRequest.builder()
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/cart/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Yêu cầu thông tin định danh người dùng qua X-User-Id header hoặc param userId"));

        verifyNoInteractions(cartService);
    }
}
