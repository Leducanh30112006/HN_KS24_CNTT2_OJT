package com.quickshop.payment.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.payment.constant.LedgerAccount;
import com.quickshop.payment.dto.ReconciliationReport;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.enums.EscrowStatus;
import com.quickshop.payment.entity.enums.LedgerTransactionType;
import com.quickshop.payment.exception.DuplicateTransactionException;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.LedgerEntryRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.service.impl.DoubleEntryLedgerServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoubleEntryLedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private EscrowAccountRepository escrowAccountRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private DoubleEntryLedgerServiceImpl ledgerService;

    @Test
    @DisplayName("1. Ghi bút toán kép thành công: Nợ CUSTOMER_PAYMENT | Có SYSTEM_ESCROW")
    void testRecordEntry_Success() {
        String txId = "ESCROW_HOLD_SUB_101";
        BigDecimal amount = new BigDecimal("300000");

        when(ledgerEntryRepository.existsByTransactionId(txId)).thenReturn(false);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        LedgerEntry entry = ledgerService.recordEntry(
                txId,
                LedgerTransactionType.ESCROW_HOLD,
                LedgerAccount.CUSTOMER_PAYMENT,
                LedgerAccount.SYSTEM_ESCROW,
                amount,
                "Tạm giữ tiền đơn hàng SubOrder #101"
        );

        assertNotNull(entry);
        assertEquals(txId, entry.getTransactionId());
        assertEquals(LedgerTransactionType.ESCROW_HOLD, entry.getTransactionType());
        assertEquals(LedgerAccount.CUSTOMER_PAYMENT, entry.getAccountDebit());
        assertEquals(LedgerAccount.SYSTEM_ESCROW, entry.getAccountCredit());
        assertEquals(amount, entry.getAmount());

        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("2. Không ghi trùng một giao dịch tài chính: Ném DuplicateTransactionException nếu txId đã tồn tại")
    void testRecordEntry_DuplicateTransaction_ThrowsException() {
        String txId = "ESCROW_HOLD_SUB_101";
        BigDecimal amount = new BigDecimal("300000");

        when(ledgerEntryRepository.existsByTransactionId(txId)).thenReturn(true);

        assertThrows(DuplicateTransactionException.class, () -> ledgerService.recordEntry(
                txId,
                LedgerTransactionType.ESCROW_HOLD,
                LedgerAccount.CUSTOMER_PAYMENT,
                LedgerAccount.SYSTEM_ESCROW,
                amount,
                "Tạm giữ tiền lặp"
        ));

        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Ném BadRequestException nếu số tiền bút toán nhỏ hơn hoặc bằng 0")
    void testRecordEntry_InvalidAmount_ThrowsException() {
        assertThrows(BadRequestException.class, () -> ledgerService.recordEntry(
                "TX_INVALID_AMOUNT",
                LedgerTransactionType.ESCROW_HOLD,
                LedgerAccount.CUSTOMER_PAYMENT,
                LedgerAccount.SYSTEM_ESCROW,
                BigDecimal.ZERO,
                "Số tiền 0"
        ));
    }

    @Test
    @DisplayName("4. Ném BadRequestException nếu tài khoản Nợ trùng tài khoản Có")
    void testRecordEntry_SameDebitAndCredit_ThrowsException() {
        assertThrows(BadRequestException.class, () -> ledgerService.recordEntry(
                "TX_SAME_ACCOUNT",
                LedgerTransactionType.ESCROW_HOLD,
                LedgerAccount.SYSTEM_ESCROW,
                LedgerAccount.SYSTEM_ESCROW,
                new BigDecimal("100000"),
                "Tài khoản trùng nhau"
        ));
    }

    @Test
    @DisplayName("5. Đối soát toàn diện hệ thống: Khớp số dư (BALANCED) khi số dư sổ cái bằng bảng escrow_accounts")
    void testReconciliation_Balanced() {
        BigDecimal totalLedger = new BigDecimal("1000000");
        when(ledgerEntryRepository.sumTotalAmount()).thenReturn(totalLedger);

        // SYSTEM_ESCROW phát sinh Có 500,000, phát sinh Nợ 200,000 -> Kỳ vọng HOLDING = 300,000
        when(ledgerEntryRepository.sumAmountByAccountCredit(LedgerAccount.SYSTEM_ESCROW)).thenReturn(new BigDecimal("500000"));
        when(ledgerEntryRepository.sumAmountByAccountDebit(LedgerAccount.SYSTEM_ESCROW)).thenReturn(new BigDecimal("200000"));

        // Bảng escrow_accounts có tổng trạng thái HOLDING = 300,000
        when(escrowAccountRepository.sumAmountByStatus(EscrowStatus.HOLDING)).thenReturn(new BigDecimal("300000"));

        ReconciliationReport report = ledgerService.reconcileAll();

        assertNotNull(report);
        assertEquals("BALANCED", report.getStatus());
        assertTrue(report.isDoubleEntryBalanced());
        assertEquals("BALANCED", report.getEscrowStatus());
        assertEquals(new BigDecimal("300000"), report.getExpectedEscrowHolding());
        assertEquals(new BigDecimal("300000"), report.getActualEscrowHolding());
        assertEquals(BigDecimal.ZERO, report.getEscrowDiscrepancy());
    }

    @Test
    @DisplayName("6. Đối soát toàn diện hệ thống: Phát hiện sai lệch (DISCREPANCY) khi số dư không khớp")
    void testReconciliation_DiscrepancyDetected() {
        when(ledgerEntryRepository.sumTotalAmount()).thenReturn(new BigDecimal("1000000"));

        when(ledgerEntryRepository.sumAmountByAccountCredit(LedgerAccount.SYSTEM_ESCROW)).thenReturn(new BigDecimal("500000"));
        when(ledgerEntryRepository.sumAmountByAccountDebit(LedgerAccount.SYSTEM_ESCROW)).thenReturn(new BigDecimal("200000")); // Kỳ vọng 300,000

        // Bảng escrow_accounts thực tế chỉ có 250,000 (lệch 50,000)
        when(escrowAccountRepository.sumAmountByStatus(EscrowStatus.HOLDING)).thenReturn(new BigDecimal("250000"));

        ReconciliationReport report = ledgerService.reconcileAll();

        assertNotNull(report);
        assertEquals("DISCREPANCY", report.getStatus());
        assertEquals("DISCREPANCY", report.getEscrowStatus());
        assertEquals(new BigDecimal("300000"), report.getExpectedEscrowHolding());
        assertEquals(new BigDecimal("250000"), report.getActualEscrowHolding());
        assertEquals(new BigDecimal("-50000"), report.getEscrowDiscrepancy());
    }

    @Test
    @DisplayName("7. Đối soát Ví Shop: So sánh số dư ví với phát sinh Sổ cái (Credit - Debit)")
    void testReconcileShopWallet() {
        Long shopId = 10L;
        String sellerAccount = LedgerAccount.sellerWallet(shopId);

        // Sổ cái: Ghi Có 500,000 (tiền hàng về), Ghi Nợ 100,000 (rút tiền) -> Kỳ vọng 400,000
        when(ledgerEntryRepository.sumAmountByAccountCredit(sellerAccount)).thenReturn(new BigDecimal("500000"));
        when(ledgerEntryRepository.sumAmountByAccountDebit(sellerAccount)).thenReturn(new BigDecimal("100000"));

        Wallet wallet = Wallet.builder()
                .shopId(shopId)
                .balance(new BigDecimal("400000"))
                .build();
        when(walletRepository.findByShopId(shopId)).thenReturn(Optional.of(wallet));

        boolean isBalanced = ledgerService.reconcileShopWallet(shopId);
        assertTrue(isBalanced, "Số dư ví phải khớp với sổ cái");
    }
}
