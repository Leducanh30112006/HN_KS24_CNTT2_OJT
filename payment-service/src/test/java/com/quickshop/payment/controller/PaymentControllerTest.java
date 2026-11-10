package com.quickshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.service.LedgerService;
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
import java.util.List;
import java.util.Map;

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

    @MockBean
    private LedgerService ledgerService;

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

    @Test
    @DisplayName("API GET /api/payments/ledger/audit trả về 200 kèm báo cáo cân đối sổ cái")
    void testAuditLedgerApi() throws Exception {
        LedgerAuditReportDto mockReport = LedgerAuditReportDto.builder()
                .isBalanced(true)
                .totalDebits(BigDecimal.valueOf(1000000))
                .totalCredits(BigDecimal.valueOf(1000000))
                .discrepancy(BigDecimal.ZERO)
                .totalEntries(4)
                .status("BALANCED")
                .accountBalances(Map.of("CUSTOMER_PAYMENT", BigDecimal.valueOf(500000)))
                .auditedAt(LocalDateTime.now())
                .message("Sổ cái kép cân bằng")
                .build();

        when(ledgerService.auditLedger()).thenReturn(mockReport);

        mockMvc.perform(get("/api/payments/ledger/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balanced").value(true))
                .andExpect(jsonPath("$.data.status").value("BALANCED"))
                .andExpect(jsonPath("$.data.totalDebits").value(1000000));
    }

    @Test
    @DisplayName("API GET /api/payments/ledger/accounts/{account} trả về 200 kèm số dư Nợ/Có")
    void testGetAccountBalanceApi() throws Exception {
        AccountBalanceDto mockBalance = AccountBalanceDto.builder()
                .account("ESCROW_HOLDING")
                .totalDebit(BigDecimal.valueOf(1500000))
                .totalCredit(BigDecimal.valueOf(1000000))
                .netBalance(BigDecimal.valueOf(500000))
                .build();

        when(ledgerService.getAccountBalance("ESCROW_HOLDING")).thenReturn(mockBalance);

        mockMvc.perform(get("/api/payments/ledger/accounts/ESCROW_HOLDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.account").value("ESCROW_HOLDING"))
                .andExpect(jsonPath("$.data.netBalance").value(500000));
    }

    @Test
    @DisplayName("API GET /api/payments/ledger/verify/{transactionId} trả về 200 xác nhận bất biến")
    void testVerifyTransactionApi() throws Exception {
        when(ledgerService.verifyTransactionInvariant("TXN_123")).thenReturn(true);

        mockMvc.perform(get("/api/payments/ledger/verify/TXN_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}
