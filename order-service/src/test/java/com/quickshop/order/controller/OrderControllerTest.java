package com.quickshop.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.order.dto.checkout.CheckoutItemRequest;
import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.order.MasterOrderResponse;
import com.quickshop.order.dto.order.SubOrderResponse;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.PaymentMethod;
import com.quickshop.order.entity.enums.SubOrderStatus;
import com.quickshop.order.exception.GlobalExceptionHandler;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CheckoutService checkoutService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final Long USER_ID = 1001L;

    @Test
    @DisplayName("POST /api/orders/checkout thành công -> Trả về 201 Created và MasterOrder")
    void testCheckout_Success_Returns201() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("123 Lê Duẩn, Hà Nội")
                .recipientName("Nguyễn Văn Kiên")
                .recipientPhone("0987654321")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(CheckoutItemRequest.builder().variantId(201L).quantity(2).build()))
                .build();

        MasterOrderResponse response = MasterOrderResponse.builder()
                .id(101L)
                .userId(USER_ID)
                .totalAmount(BigDecimal.valueOf(1030000))
                .voucherDiscount(BigDecimal.ZERO)
                .shippingFeeTotal(BigDecimal.valueOf(30000))
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.COD)
                .shippingAddress("123 Lê Duẩn, Hà Nội")
                .recipientName("Nguyễn Văn Kiên")
                .recipientPhone("0987654321")
                .createdAt(LocalDateTime.now())
                .subOrders(List.of(
                        SubOrderResponse.builder()
                                .id(1L)
                                .masterOrderId(101L)
                                .shopId(10L)
                                .subtotalAmount(BigDecimal.valueOf(1000000))
                                .shippingFee(BigDecimal.valueOf(30000))
                                .shopVoucherDiscount(BigDecimal.ZERO)
                                .finalAmount(BigDecimal.valueOf(1030000))
                                .status(SubOrderStatus.CREATED)
                                .items(Collections.emptyList())
                                .build()
                ))
                .duplicateRequest(false)
                .build();

        when(checkoutService.checkout(eq(USER_ID), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đặt hàng thành công"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.totalAmount").value(1030000))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.subOrders.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/orders/checkout với request lặp -> Trả về 200 OK và duplicateRequest = true")
    void testCheckout_DuplicateRequest_Returns200() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .requestId("req-duplicate-999")
                .shippingAddress("123 Lê Duẩn, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .build();

        MasterOrderResponse duplicateResponse = MasterOrderResponse.builder()
                .id(101L)
                .userId(USER_ID)
                .totalAmount(BigDecimal.valueOf(1030000))
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.COD)
                .shippingAddress("123 Lê Duẩn, Hà Nội")
                .subOrders(Collections.emptyList())
                .duplicateRequest(true)
                .build();

        when(checkoutService.checkout(eq(USER_ID), any(), any())).thenReturn(duplicateResponse);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("X-User-Id", USER_ID)
                        .header("X-Idempotency-Key", "req-duplicate-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Yêu cầu checkout lặp lại - trả về đơn hàng đã tạo trước đó"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.duplicateRequest").value(true));
    }

    @Test
    @DisplayName("POST /api/orders/checkout thiếu User ID -> Trả về 400 Bad Request")
    void testCheckout_MissingUserId_Returns400() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("123 Lê Duẩn, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .build();

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Yêu cầu thông tin định danh người dùng qua X-User-Id header hoặc param userId"));
    }

    @Test
    @DisplayName("POST /api/orders/checkout thiếu địa chỉ hoặc phương thức thanh toán -> Trả về 400 Validation Error")
    void testCheckout_MissingRequiredFields_Returns400() throws Exception {
        CheckoutRequest invalidRequest = CheckoutRequest.builder()
                .shippingAddress("") // Thiếu địa chỉ
                .paymentMethod(null) // Thiếu payment method
                .build();

        mockMvc.perform(post("/api/orders/checkout")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu đầu vào không hợp lệ"));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} thành công -> Trả về 200 OK")
    void testGetOrderById_Success_Returns200() throws Exception {
        MasterOrderResponse response = MasterOrderResponse.builder()
                .id(101L)
                .userId(USER_ID)
                .totalAmount(BigDecimal.valueOf(500000))
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.COD)
                .shippingAddress("Hà Nội")
                .subOrders(Collections.emptyList())
                .build();

        when(checkoutService.getOrderById(USER_ID, 101L)).thenReturn(response);

        mockMvc.perform(get("/api/orders/101")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.totalAmount").value(500000));
    }
}
