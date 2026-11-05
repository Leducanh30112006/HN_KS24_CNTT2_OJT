package com.quickshop.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.order.dto.checkout.CheckoutItemDto;
import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.checkout.CheckoutResponse;
import com.quickshop.order.dto.checkout.SubOrderResponse;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.MasterOrderStatus;
import com.quickshop.order.entity.OrderItem;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.SubOrderStatus;
import com.quickshop.order.exception.GlobalExceptionHandler;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.OrderItemRepository;
import com.quickshop.order.repository.SubOrderRepository;
import com.quickshop.order.service.CheckoutService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MasterOrderRepository masterOrderRepository;

    @MockBean
    private SubOrderRepository subOrderRepository;

    @MockBean
    private OrderItemRepository orderItemRepository;

    @MockBean
    private CheckoutService checkoutService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("POST /api/orders/checkout -> Thành công trả về 201 Created kèm dữ liệu CheckoutResponse")
    void testCheckout_Success() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod("COD")
                .recipientName("Nguyen Van Kien")
                .recipientPhone("0987654321")
                .shippingAddress("123 Duong Le Loi, Q1, TP.HCM")
                .note("Giao gio hanh chinh")
                .items(List.of(
                        CheckoutItemDto.builder().variantId(101L).quantity(2).build()
                ))
                .build();

        CheckoutResponse mockResponse = CheckoutResponse.builder()
                .masterOrderId(1001L)
                .orderCode("QS-101001")
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(325000))
                .subtotalAmount(BigDecimal.valueOf(300000))
                .shippingFeeTotal(BigDecimal.valueOf(25000))
                .status(MasterOrderStatus.PROCESSING)
                .paymentMethod("COD")
                .recipientName("Nguyen Van Kien")
                .recipientPhone("0987654321")
                .shippingAddress("123 Duong Le Loi, Q1, TP.HCM")
                .totalShops(1)
                .totalItems(2)
                .createdAt(LocalDateTime.now())
                .subOrders(List.of(
                        SubOrderResponse.builder()
                                .id(5001L)
                                .subOrderCode("QS-SUB-105001")
                                .shopId(10L)
                                .shopName("Shop A")
                                .subtotalAmount(BigDecimal.valueOf(300000))
                                .shippingFee(BigDecimal.valueOf(25000))
                                .finalAmount(BigDecimal.valueOf(325000))
                                .status(SubOrderStatus.CREATED)
                                .build()
                ))
                .build();

        when(checkoutService.checkout(eq(1L), any(CheckoutRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.masterOrderId").value(1001))
                .andExpect(jsonPath("$.data.orderCode").value("QS-101001"))
                .andExpect(jsonPath("$.data.totalAmount").value(325000))
                .andExpect(jsonPath("$.data.subOrders[0].shopId").value(10));
    }

    @Test
    @DisplayName("POST /api/orders/checkout/preview -> Thành công trả về 200 OK")
    void testPreviewCheckout_Success() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod("COD")
                .recipientName("Nguyen Van Kien")
                .recipientPhone("0987654321")
                .shippingAddress("123 Duong Le Loi, Q1, TP.HCM")
                .items(List.of(
                        CheckoutItemDto.builder().variantId(101L).quantity(1).build()
                ))
                .build();

        CheckoutResponse mockResponse = CheckoutResponse.builder()
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(125000))
                .subtotalAmount(BigDecimal.valueOf(100000))
                .shippingFeeTotal(BigDecimal.valueOf(25000))
                .status(MasterOrderStatus.DRAFT)
                .paymentMethod("COD")
                .totalShops(1)
                .totalItems(1)
                .build();

        when(checkoutService.previewCheckout(eq(1L), any(CheckoutRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/orders/checkout/preview")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(125000))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/orders/checkout -> Thất bại khi thiếu trường bắt buộc (@Valid)")
    void testCheckout_ValidationFailure() throws Exception {
        CheckoutRequest invalidRequest = CheckoutRequest.builder()
                .recipientName("") // Bắt buộc
                .recipientPhone("") // Bắt buộc
                .shippingAddress("") // Bắt buộc
                .paymentMethod("") // Bắt buộc
                .build();

        mockMvc.perform(post("/api/orders/checkout")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/orders/{id} -> Thành công trả về chi tiết MasterOrder kèm SubOrders và OrderItems")
    void testGetOrderDetails_Success() throws Exception {
        MasterOrder masterOrder = MasterOrder.builder()
                .id(101L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(325000))
                .status(MasterOrderStatus.PROCESSING)
                .paymentMethod("COD")
                .shippingAddress("123 Duong Le Loi, Q1")
                .createdAt(LocalDateTime.now())
                .build();

        SubOrder subOrder = SubOrder.builder()
                .id(501L)
                .masterOrderId(101L)
                .shopId(10L)
                .subtotalAmount(BigDecimal.valueOf(300000))
                .shippingFee(BigDecimal.valueOf(25000))
                .finalAmount(BigDecimal.valueOf(325000))
                .status(SubOrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .id(901L)
                .subOrderId(501L)
                .variantId(11L)
                .productId(22L)
                .productName("Áo Khoác")
                .sku("JACKET-01")
                .price(BigDecimal.valueOf(300000))
                .quantity(1)
                .build();

        when(masterOrderRepository.findById(101L)).thenReturn(Optional.of(masterOrder));
        when(subOrderRepository.findByMasterOrderId(101L)).thenReturn(List.of(subOrder));
        when(orderItemRepository.findBySubOrderId(501L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/orders/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.masterOrder.id").value(101))
                .andExpect(jsonPath("$.data.subOrders[0].subOrder.id").value(501))
                .andExpect(jsonPath("$.data.subOrders[0].items[0].sku").value("JACKET-01"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} -> Ném 404 Not Found khi đơn hàng không tồn tại")
    void testGetOrderDetails_NotFound() throws Exception {
        when(masterOrderRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/orders -> Trả về danh sách đơn hàng của người dùng theo X-User-Id")
    void testGetMyOrders_Success() throws Exception {
        MasterOrder order1 = MasterOrder.builder().id(1L).userId(1L).totalAmount(BigDecimal.valueOf(100000)).build();
        MasterOrder order2 = MasterOrder.builder().id(2L).userId(1L).totalAmount(BigDecimal.valueOf(200000)).build();

        when(masterOrderRepository.findByUserId(1L)).thenReturn(List.of(order1, order2));

        mockMvc.perform(get("/api/orders").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
