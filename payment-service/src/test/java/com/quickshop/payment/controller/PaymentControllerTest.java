package com.quickshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.payment.dto.HoldEscrowRequest;
import com.quickshop.payment.dto.ReleaseEscrowRequest;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.dto.WithdrawalRequestDto;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    @DisplayName("API GET /api/payments/wallet/{shopId} trả về 200")
    void testGetWalletApi() throws Exception {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .shopId(501L)
                .balance(BigDecimal.valueOf(500000))
                .frozenBalance(BigDecimal.ZERO)
                .build();

        when(paymentService.getWalletByShopId(501L)).thenReturn(wallet);

        mockMvc.perform(get("/api/payments/wallet/501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(500000));
    }

    @Test
    @DisplayName("API POST /api/payments/escrow/hold trả về 200")
    void testHoldEscrowApi() throws Exception {
        HoldEscrowRequest req = HoldEscrowRequest.builder()
                .subOrderId(2001L)
                .shopId(501L)
                .amount(BigDecimal.valueOf(400000))
                .commissionFee(BigDecimal.valueOf(20000))
                .build();

        EscrowAccount escrow = EscrowAccount.builder()
                .id(1L)
                .subOrderId(2001L)
                .amount(BigDecimal.valueOf(400000))
                .commissionFee(BigDecimal.valueOf(20000))
                .status("HOLDING")
                .build();

        when(paymentService.holdEscrow(any(HoldEscrowRequest.class))).thenReturn(escrow);

        mockMvc.perform(post("/api/payments/escrow/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("HOLDING"));
    }

    @Test
    @DisplayName("API POST /api/payments/escrow/release trả về 200")
    void testReleaseEscrowApi() throws Exception {
        ReleaseEscrowRequest req = ReleaseEscrowRequest.builder()
                .subOrderId(2001L)
                .shopId(501L)
                .build();

        EscrowAccount escrow = EscrowAccount.builder()
                .id(1L)
                .subOrderId(2001L)
                .amount(BigDecimal.valueOf(400000))
                .status("RELEASED")
                .build();

        when(paymentService.releaseEscrow(any(ReleaseEscrowRequest.class))).thenReturn(escrow);

        mockMvc.perform(post("/api/payments/escrow/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RELEASED"));
    }

    @Test
    @DisplayName("API POST /api/payments/withdrawals trả về 200")
    void testWithdrawalsApi() throws Exception {
        WithdrawalRequestDto req = WithdrawalRequestDto.builder()
                .shopId(501L)
                .amount(BigDecimal.valueOf(200000))
                .bankCode("MB")
                .bankAccountNumber("99998888")
                .bankAccountName("SHOP A")
                .build();

        Withdrawal w = Withdrawal.builder()
                .id(1L)
                .shopId(501L)
                .amount(BigDecimal.valueOf(200000))
                .status("PENDING")
                .build();

        when(paymentService.requestWithdrawal(any(WithdrawalRequestDto.class))).thenReturn(w);

        mockMvc.perform(post("/api/payments/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("API POST /api/payments/vnpay-create trả về 200 kèm url")
    void testCreateVNPayUrlApi() throws Exception {
        VNPayPaymentResponse mockResponse = VNPayPaymentResponse.builder()
                .orderId("ORD-123")
                .txnRef("ORD-123_1700000000000")
                .amount(BigDecimal.valueOf(500000))
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=ORD-123_1700000000000&vnp_SecureHash=abcdef")
                .build();

        when(paymentService.createVNPayPayment(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/vnpay-create")
                        .param("orderId", "ORD-123")
                        .param("amount", "500000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("ORD-123"))
                .andExpect(jsonPath("$.data.paymentUrl").isNotEmpty());
    }
}
