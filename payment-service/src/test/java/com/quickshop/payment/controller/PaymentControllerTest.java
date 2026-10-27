package com.quickshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.payment.dto.VNPayCreatePaymentRequest;
import com.quickshop.payment.dto.VNPayIpnResponse;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.dto.VNPayReturnResponse;
import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.entity.enums.PaymentMethod;
import com.quickshop.payment.entity.enums.PaymentStatus;
import com.quickshop.payment.exception.GlobalExceptionHandler;
import com.quickshop.payment.service.VNPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VNPayService vnPayService;

    @InjectMocks
    private PaymentController paymentController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. POST /api/payments/vnpay/create: Tạo URL thanh toán thành công trả HTTP 200")
    void testCreatePaymentUrl_Success() throws Exception {
        VNPayCreatePaymentRequest request = VNPayCreatePaymentRequest.builder()
                .orderId(1001L)
                .amount(new BigDecimal("200000"))
                .orderInfo("Thanh toan don hang 1001")
                .build();

        VNPayPaymentResponse response = VNPayPaymentResponse.builder()
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=20000000&vnp_SecureHash=abc123")
                .txnRef("1001_1700000000")
                .orderId(1001L)
                .amount(new BigDecimal("200000"))
                .build();

        when(vnPayService.createPaymentUrl(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/vnpay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(1001))
                .andExpect(jsonPath("$.data.txnRef").value("1001_1700000000"))
                .andExpect(jsonPath("$.data.paymentUrl").isNotEmpty());
    }

    @Test
    @DisplayName("2. POST /api/payments/vnpay/create: Thiếu orderId hoặc amount < 1000 trả HTTP 400")
    void testCreatePaymentUrl_ValidationFailed() throws Exception {
        VNPayCreatePaymentRequest invalidRequest = VNPayCreatePaymentRequest.builder()
                .orderId(null)
                .amount(new BigDecimal("500"))
                .build();

        mockMvc.perform(post("/api/payments/vnpay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("3. GET /api/payments/vnpay/ipn: Nhận IPN thành công trả JSON phẳng RspCode 00")
    void testHandleIpn_Success_ReturnsRspCode00() throws Exception {
        when(vnPayService.processIpn(any())).thenReturn(new VNPayIpnResponse("00", "Confirm Success"));

        mockMvc.perform(get("/api/payments/vnpay/ipn")
                        .param("vnp_TxnRef", "1001_1700000000")
                        .param("vnp_Amount", "20000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_SecureHash", "VALID_HASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    @DisplayName("4. GET /api/payments/vnpay/ipn: Chữ ký không hợp lệ trả RspCode 97")
    void testHandleIpn_InvalidChecksum_ReturnsRspCode97() throws Exception {
        when(vnPayService.processIpn(any())).thenReturn(new VNPayIpnResponse("97", "Invalid Checksum"));

        mockMvc.perform(get("/api/payments/vnpay/ipn")
                        .param("vnp_TxnRef", "1001_1700000000")
                        .param("vnp_SecureHash", "INVALID_HASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(jsonPath("$.Message").value("Invalid Checksum"));
    }

    @Test
    @DisplayName("5. GET /api/payments/vnpay/ipn: IPN lặp trả RspCode 02 (Order already confirmed)")
    void testHandleIpn_Duplicate_ReturnsRspCode02() throws Exception {
        when(vnPayService.processIpn(any())).thenReturn(new VNPayIpnResponse("02", "Order already confirmed"));

        mockMvc.perform(get("/api/payments/vnpay/ipn")
                        .param("vnp_TxnRef", "1001_1700000000")
                        .param("vnp_SecureHash", "VALID_HASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("02"))
                .andExpect(jsonPath("$.Message").value("Order already confirmed"));
    }

    @Test
    @DisplayName("6. GET /api/payments/vnpay/return: Xử lý redirect return URL trả ApiResponse")
    void testHandleReturn_Success() throws Exception {
        VNPayReturnResponse returnResponse = VNPayReturnResponse.builder()
                .orderId(1001L)
                .txnRef("1001_1700000000")
                .amount(new BigDecimal("200000"))
                .validSignature(true)
                .success(false) // Không tin tưởng trình duyệt
                .status(PaymentStatus.PENDING)
                .message("Giao dịch đang chờ xác nhận từ IPN Webhook VNPay")
                .build();

        when(vnPayService.processReturn(any())).thenReturn(returnResponse);

        mockMvc.perform(get("/api/payments/vnpay/return")
                        .param("vnp_TxnRef", "1001_1700000000")
                        .param("vnp_SecureHash", "VALID_HASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(1001))
                .andExpect(jsonPath("$.data.validSignature").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("7. GET /api/payments/orders/{orderId}/transaction: Tra cứu giao dịch theo orderId")
    void testGetLatestTransaction() throws Exception {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(1L)
                .orderId(1001L)
                .txnRef("1001_1700000000")
                .amount(new BigDecimal("200000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(vnPayService.getLatestTransactionByOrderId(1001L)).thenReturn(tx);

        mockMvc.perform(get("/api/payments/orders/1001/transaction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(1001))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
