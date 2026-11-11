package com.quickshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.payment.dto.CreatePayOsPaymentResponse;
import com.quickshop.payment.dto.PaymentTransactionResponse;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.service.LedgerService;
import com.quickshop.payment.service.PayOsService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerPayOsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PayOsService payOsService;

    @MockBean
    private LedgerService ledgerService;

    @Test
    @DisplayName("API POST /api/payments/payos/create: Chỉ cần orderId trả về 200 kèm PayOS link")
    void testCreatePayOsPayment_WithOnlyOrderId() throws Exception {
        CreatePayOsPaymentResponse mockResponse = CreatePayOsPaymentResponse.builder()
                .orderId("1001")
                .orderCode(1001L)
                .amount(BigDecimal.valueOf(150000))
                .checkoutUrl("https://pay.payos.vn/web/test1001")
                .qrCode("https://img.vietqr.io/image/970422-0346746521-compact2.png")
                .accountNumber("0346746521")
                .accountName("NGUYEN VAN KIEN")
                .status("PENDING")
                .build();

        when(payOsService.createPayment("1001")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/payos/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"1001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("1001"))
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://pay.payos.vn/web/test1001"))
                .andExpect(jsonPath("$.data.accountNumber").value("0346746521"));
    }

    @Test
    @DisplayName("API POST /api/payments/create?orderId=1001: Chỉ nhận query param orderId")
    void testCreatePayment_WithQueryParamOrderId() throws Exception {
        CreatePayOsPaymentResponse mockResponse = CreatePayOsPaymentResponse.builder()
                .orderId("1001")
                .orderCode(1001L)
                .amount(BigDecimal.valueOf(150000))
                .checkoutUrl("https://pay.payos.vn/web/test1001")
                .qrCode("https://img.vietqr.io/image/970422-0346746521-compact2.png")
                .accountNumber("0346746521")
                .accountName("NGUYEN VAN KIEN")
                .status("PENDING")
                .build();

        when(payOsService.createPayment("1001")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/create")
                        .param("orderId", "1001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("1001"));
    }

    @Test
    @DisplayName("API GET /api/payments/status/{orderId}: Kiểm tra trạng thái thanh toán")
    void testCheckPaymentStatus() throws Exception {
        Map<String, Object> mockStatus = Map.of(
                "paid", true,
                "status", "SUCCESS",
                "orderId", "1001",
                "amount", 150000
        );

        when(payOsService.checkPaymentStatus("1001")).thenReturn(mockStatus);

        mockMvc.perform(get("/api/payments/status/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paid").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("API POST /api/payments/payos/webhook: Xử lý Webhook biến động số dư")
    void testHandlePayOsWebhook() throws Exception {
        Map<String, Object> mockResp = Map.of("error", 0, "message", "OK");
        when(payOsService.processWebhook(any())).thenReturn(mockResp);

        mockMvc.perform(post("/api/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{\"orderCode\":1001,\"amount\":150000}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(0))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    @DisplayName("API POST /api/payments/payos/confirm-webhook: Đăng ký Webhook URL với PayOS")
    void testConfirmPayOsWebhook() throws Exception {
        when(payOsService.confirmWebhook("https://test.loca.lt/webhook")).thenReturn("CONFIRMED");

        mockMvc.perform(post("/api/payments/payos/confirm-webhook")
                        .param("webhookUrl", "https://test.loca.lt/webhook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("CONFIRMED"));
    }

    @Test
    @DisplayName("API POST /api/payments/payos/cancel: Hủy liên kết thanh toán PayOS")
    void testCancelPayOsPayment() throws Exception {
        Map<String, Object> mockResp = Map.of("orderId", "1001", "transactionStatus", "CANCELLED");
        when(payOsService.cancelPaymentLink(eq("1001"), any())).thenReturn(mockResp);

        mockMvc.perform(post("/api/payments/payos/cancel")
                        .param("orderId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("API GET /api/payments/transactions/{txnRef}: Trả về 200 kèm chi tiết giao dịch")
    void testGetTransactionByTxnRef() throws Exception {
        PaymentTransactionResponse response = PaymentTransactionResponse.builder()
                .id(1L)
                .orderId("1001")
                .txnRef("1001")
                .amount(BigDecimal.valueOf(150000))
                .paymentMethod("PAYOS_MBBANK")
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.getTransactionByTxnRef("1001")).thenReturn(response);

        mockMvc.perform(get("/api/payments/transactions/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("1001"))
                .andExpect(jsonPath("$.data.paymentMethod").value("PAYOS_MBBANK"));
    }

    @Test
    @DisplayName("API GET /api/payments/order/{orderId}: Trả về 200 kèm giao dịch theo mã đơn")
    void testGetTransactionByOrderId() throws Exception {
        PaymentTransactionResponse response = PaymentTransactionResponse.builder()
                .id(1L)
                .orderId("1001")
                .txnRef("1001")
                .amount(BigDecimal.valueOf(150000))
                .paymentMethod("PAYOS_MBBANK")
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.getTransactionByOrderId("1001")).thenReturn(response);

        mockMvc.perform(get("/api/payments/order/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("1001"));
    }
}
