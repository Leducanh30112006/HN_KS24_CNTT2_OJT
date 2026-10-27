package com.quickshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.payment.dto.ReconciliationReport;
import com.quickshop.payment.dto.WalletResponse;
import com.quickshop.payment.dto.WithdrawalRequestDto;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.entity.enums.WithdrawalStatus;
import com.quickshop.payment.exception.GlobalExceptionHandler;
import com.quickshop.payment.service.DoubleEntryLedgerService;
import com.quickshop.payment.service.WalletService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LedgerAndWalletControllerTest {

    private MockMvc mockMvcLedger;
    private MockMvc mockMvcWallet;

    @Mock
    private DoubleEntryLedgerService ledgerService;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private LedgerController ledgerController;

    @InjectMocks
    private WalletController walletController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvcLedger = MockMvcBuilders.standaloneSetup(ledgerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvcWallet = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. GET /api/payments/ledger/reconciliation: Lấy báo cáo đối soát sổ cái thành công")
    void testRunReconciliation() throws Exception {
        ReconciliationReport report = ReconciliationReport.builder()
                .reconciledAt(LocalDateTime.now())
                .status("BALANCED")
                .isDoubleEntryBalanced(true)
                .totalDebits(new BigDecimal("1000000"))
                .totalCredits(new BigDecimal("1000000"))
                .escrowStatus("BALANCED")
                .expectedEscrowHolding(new BigDecimal("300000"))
                .actualEscrowHolding(new BigDecimal("300000"))
                .escrowDiscrepancy(BigDecimal.ZERO)
                .details(List.of("Khớp cân đối 100%"))
                .build();

        when(ledgerService.reconcileAll()).thenReturn(report);

        mockMvcLedger.perform(get("/api/payments/ledger/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("BALANCED"))
                .andExpect(jsonPath("$.data.doubleEntryBalanced").value(true));
    }

    @Test
    @DisplayName("2. GET /api/payments/wallets/shops/{shopId}: Tra cứu số dư ví thành công với header xác thực")
    void testGetWalletByShopId() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .shopId(10L)
                .balance(new BigDecimal("500000"))
                .frozenBalance(BigDecimal.ZERO)
                .totalBalance(new BigDecimal("500000"))
                .build();

        when(walletService.getWalletByShopId(anyLong(), any(), any())).thenReturn(response);

        mockMvcWallet.perform(get("/api/payments/wallets/shops/10")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ROLE_SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shopId").value(10))
                .andExpect(jsonPath("$.data.balance").value(500000));
    }

    @Test
    @DisplayName("3. POST /api/payments/wallets/withdraw: Tạo lệnh rút tiền thành công")
    void testRequestWithdrawal() throws Exception {
        WithdrawalRequestDto request = WithdrawalRequestDto.builder()
                .shopId(10L)
                .amount(new BigDecimal("200000"))
                .bankCode("VCB")
                .bankAccountNumber("0123456789")
                .bankAccountName("NGUYEN VAN KIEN")
                .idempotencyKey("IDEMP_001")
                .build();

        Withdrawal withdrawal = Withdrawal.builder()
                .id(501L)
                .shopId(10L)
                .amount(new BigDecimal("200000"))
                .status(WithdrawalStatus.PENDING)
                .build();

        when(walletService.requestWithdrawal(any(), any(), any())).thenReturn(withdrawal);

        mockMvcWallet.perform(post("/api/payments/wallets/withdraw")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ROLE_SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("4. POST /api/payments/wallets/withdrawals/{id}/approve: Admin phê duyệt lệnh rút tiền thành công")
    void testApproveWithdrawal() throws Exception {
        Withdrawal withdrawal = Withdrawal.builder()
                .id(501L)
                .shopId(10L)
                .amount(new BigDecimal("200000"))
                .status(WithdrawalStatus.COMPLETED)
                .adminId(1L)
                .bankTransactionRef("MOCK_NAPAS247_123")
                .build();

        when(walletService.approveWithdrawal(anyLong(), anyLong(), any())).thenReturn(withdrawal);

        mockMvcWallet.perform(post("/api/payments/wallets/withdrawals/501/approve")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ROLE_ADMIN")
                        .param("adminId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("5. POST /api/payments/wallets/withdrawals/{id}/reject: Admin từ chối lệnh rút tiền")
    void testRejectWithdrawal() throws Exception {
        Withdrawal withdrawal = Withdrawal.builder()
                .id(501L)
                .shopId(10L)
                .amount(new BigDecimal("200000"))
                .status(WithdrawalStatus.REJECTED)
                .rejectionReason("Sai thông tin tài khoản")
                .build();

        when(walletService.rejectWithdrawal(anyLong(), anyLong(), any(), any())).thenReturn(withdrawal);

        mockMvcWallet.perform(post("/api/payments/wallets/withdrawals/501/reject")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ROLE_ADMIN")
                        .param("reason", "Sai thông tin tài khoản"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}
