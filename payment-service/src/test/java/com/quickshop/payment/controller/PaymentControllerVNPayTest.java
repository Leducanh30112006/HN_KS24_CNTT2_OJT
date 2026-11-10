package com.quickshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.payment.dto.CreateVNPayPaymentRequest;
import com.quickshop.payment.dto.PaymentTransactionResponse;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerVNPayTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    @DisplayName("API POST /api/payments/vnpay/create: Thành công trả về 200 kèm VNPayPaymentResponse")
    void testCreateVNPayPaymentSuccess() throws Exception {
        CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                .orderId("ORDER_2026_001")
                .amount(BigDecimal.valueOf(350000))
                .userId(15L)
                .orderInfo("Thanh toan don hang test")
                .bankCode("NCB")
                .build();

        VNPayPaymentResponse mockResponse = VNPayPaymentResponse.builder()
                .txnRef("ORDER_2026_001_123456789")
                .orderId("ORDER_2026_001")
                .amount(BigDecimal.valueOf(350000))
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=ORDER_2026_001_123456789")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusMinutes(15))
                .message("Tạo URL thanh toán VNPay thành công")
                .build();

        when(paymentService.createVNPayPayment(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/vnpay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("ORDER_2026_001"))
                .andExpect(jsonPath("$.data.txnRef").value("ORDER_2026_001_123456789"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentUrl").isNotEmpty());
    }

    @Test
    @DisplayName("API POST /api/payments/vnpay/create: Validation lỗi trả về 400 Bad Request")
    void testCreateVNPayPaymentValidationFailure() throws Exception {
        // Thiếu orderId và amount < 1000
        CreateVNPayPaymentRequest invalidRequest = CreateVNPayPaymentRequest.builder()
                .amount(BigDecimal.valueOf(500))
                .build();

        mockMvc.perform(post("/api/payments/vnpay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API GET /api/payments/transactions/{txnRef}: Trả về 200 kèm chi tiết giao dịch")
    void testGetTransactionByTxnRef() throws Exception {
        PaymentTransactionResponse mockResponse = PaymentTransactionResponse.builder()
                .id(1L)
                .txnRef("TXN_123456")
                .orderId("ORDER_555")
                .userId(10L)
                .amount(BigDecimal.valueOf(200000))
                .paymentMethod("VNPAY")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentService.getTransactionByTxnRef("TXN_123456")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/payments/transactions/TXN_123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.txnRef").value("TXN_123456"))
                .andExpect(jsonPath("$.data.orderId").value("ORDER_555"));
    }

    @Test
    @DisplayName("API GET /api/payments/order/{orderId}: Trả về 200 kèm giao dịch theo mã đơn")
    void testGetTransactionByOrderId() throws Exception {
        PaymentTransactionResponse mockResponse = PaymentTransactionResponse.builder()
                .id(1L)
                .txnRef("TXN_ORDER_777")
                .orderId("777")
                .userId(20L)
                .amount(BigDecimal.valueOf(500000))
                .paymentMethod("VNPAY")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentService.getTransactionByOrderId("777")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/payments/order/777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("777"))
                .andExpect(jsonPath("$.data.txnRef").value("TXN_ORDER_777"));
    }
}
