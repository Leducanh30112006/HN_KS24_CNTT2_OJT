package com.quickshop.payment.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.payment.dto.AccountBalanceDto;
import com.quickshop.payment.dto.LedgerAuditReportDto;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.repository.LedgerEntryRepository;
import com.quickshop.payment.service.impl.LedgerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerInvariantTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Nested
    @DisplayName("Kiểm tra tính bất biến kế toán kép đơn (Simple Double-Entry Invariant)")
    class SimpleDoubleEntryTests {

        @Test
        @DisplayName("Bút toán kép hợp lệ: Nợ/Có cân bằng hoàn hảo, bảo toàn phương trình kế toán")
        void testDoubleEntry_Balanced() {
            String txnId = "TXN-VNPAY-001";
            BigDecimal amount = BigDecimal.valueOf(500000);

            LedgerEntry savedEntry = LedgerEntry.builder()
                    .id(1L)
                    .transactionId(txnId)
                    .transactionType("PAYMENT_VNPAY_SUCCESS")
                    .accountDebit("VNPAY_GATEWAY")
                    .accountCredit("ESCROW_HOLDING")
                    .amount(amount)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(savedEntry);
            when(ledgerEntryRepository.findByTransactionId(txnId)).thenReturn(List.of(savedEntry));

            LedgerEntry result = ledgerService.recordDoubleEntry(
                    txnId,
                    "PAYMENT_VNPAY_SUCCESS",
                    "VNPAY_GATEWAY",
                    "ESCROW_HOLDING",
                    amount
            );

            assertThat(result).isNotNull();
            assertThat(result.getTransactionId()).isEqualTo(txnId);
            assertThat(result.getAccountDebit()).isEqualTo("VNPAY_GATEWAY");
            assertThat(result.getAccountCredit()).isEqualTo("ESCROW_HOLDING");
            assertThat(result.getAmount()).isEqualByComparingTo(amount);

            // Xác minh tính bất biến kế toán
            boolean invariant = ledgerService.verifyTransactionInvariant(txnId);
            assertThat(invariant).isTrue();
            verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
        }

        @Test
        @DisplayName("Bút toán kép thất bại khi tài khoản Nợ và Có trùng nhau (vi phạm nguyên tắc kế toán)")
        void testDoubleEntry_SameDebitAndCreditAccount_ThrowsBadRequest() {
            assertThatThrownBy(() -> ledgerService.recordDoubleEntry(
                    "TXN-ERR-001",
                    "INVALID_TRANSFER",
                    "ESCROW_HOLDING",
                    "ESCROW_HOLDING",
                    BigDecimal.valueOf(100000)
            )).isInstanceOf(BadRequestException.class)
              .hasMessageContaining("không được trùng nhau");

            verify(ledgerEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Bút toán kép thất bại khi số tiền nhỏ hơn hoặc bằng 0")
        void testDoubleEntry_InvalidAmount_ThrowsBadRequest() {
            assertThatThrownBy(() -> ledgerService.recordDoubleEntry(
                    "TXN-ERR-002",
                    "INVALID_ZERO",
                    "CUSTOMER_PAYMENT",
                    "ESCROW_HOLDING",
                    BigDecimal.ZERO
            )).isInstanceOf(BadRequestException.class)
              .hasMessageContaining("Số tiền bút toán phải lớn hơn 0");

            assertThatThrownBy(() -> ledgerService.recordDoubleEntry(
                    "TXN-ERR-003",
                    "INVALID_NEGATIVE",
                    "CUSTOMER_PAYMENT",
                    "ESCROW_HOLDING",
                    BigDecimal.valueOf(-50000)
            )).isInstanceOf(BadRequestException.class)
              .hasMessageContaining("Số tiền bút toán phải lớn hơn 0");

            verify(ledgerEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Bút toán kép thất bại khi tham số bắt buộc bị để trống")
        void testDoubleEntry_EmptyParams_ThrowsBadRequest() {
            assertThatThrownBy(() -> ledgerService.recordDoubleEntry(
                    "",
                    "ESCROW_HOLD",
                    "CUSTOMER_PAYMENT",
                    "ESCROW_HOLDING",
                    BigDecimal.valueOf(100000)
            )).isInstanceOf(BadRequestException.class);

            assertThatThrownBy(() -> ledgerService.recordDoubleEntry(
                    "TXN-ERR-004",
                    "ESCROW_HOLD",
                    "",
                    "ESCROW_HOLDING",
                    BigDecimal.valueOf(100000)
            )).isInstanceOf(BadRequestException.class);

            assertThatThrownBy(() -> ledgerService.recordDoubleEntry(
                    "TXN-ERR-005",
                    "ESCROW_HOLD",
                    "CUSTOMER_PAYMENT",
                    null,
                    BigDecimal.valueOf(100000)
            )).isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Kiểm tra tính bất biến bút toán phức hợp phân bổ hoa hồng (Compound Split-Credit Invariant)")
    class CompoundSplitCreditTests {

        @Test
        @DisplayName("Giải phóng Escrow có hoa hồng sàn: Tổng Nợ (Escrow) = Tổng Có (Shop Net + Platform Commission)")
        void testSplitCredit_WithCommission_StrictBalance() {
            String txnId = "ESCROW-REL-9001";
            BigDecimal totalEscrow = BigDecimal.valueOf(1000000);
            BigDecimal netShop = BigDecimal.valueOf(950000); // 95%
            BigDecimal commission = BigDecimal.valueOf(50000); // 5%

            Map<String, BigDecimal> splits = new LinkedHashMap<>();
            splits.put("SHOP_WALLET_101", netShop);
            splits.put("PLATFORM_REVENUE", commission);

            LedgerEntry entryShop = LedgerEntry.builder()
                    .id(10L)
                    .transactionId(txnId)
                    .transactionType("ESCROW_RELEASE")
                    .accountDebit("ESCROW_HOLDING")
                    .accountCredit("SHOP_WALLET_101")
                    .amount(netShop)
                    .build();

            LedgerEntry entryCommission = LedgerEntry.builder()
                    .id(11L)
                    .transactionId(txnId)
                    .transactionType("ESCROW_RELEASE")
                    .accountDebit("ESCROW_HOLDING")
                    .accountCredit("PLATFORM_REVENUE")
                    .amount(commission)
                    .build();

            when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                    .thenReturn(entryShop)
                    .thenReturn(entryCommission);

            when(ledgerEntryRepository.findByTransactionId(txnId))
                    .thenReturn(List.of(entryShop, entryCommission));

            List<LedgerEntry> entries = ledgerService.recordSplitCreditEntry(
                    txnId,
                    "ESCROW_RELEASE",
                    "ESCROW_HOLDING",
                    splits
            );

            assertThat(entries).hasSize(2);

            // Kiểm tra bảo toàn phương trình kế toán:
            // Tổng Nợ = 950,000 + 50,000 = 1,000,000
            // Tổng Có = 950,000 (Shop) + 50,000 (Platform) = 1,000,000
            BigDecimal totalDebit = entries.stream().map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalDebit).isEqualByComparingTo(totalEscrow);

            boolean invariantValid = ledgerService.verifyTransactionInvariant(txnId);
            assertThat(invariantValid).isTrue();
            verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        }

        @Test
        @DisplayName("Giải phóng Escrow không thu hoa hồng (0%): Toàn bộ chuyển cho Shop, bảo toàn Nợ/Có")
        void testSplitCredit_ZeroCommission_IgnoredCleanly() {
            String txnId = "ESCROW-REL-9002";
            BigDecimal netShop = BigDecimal.valueOf(300000);

            Map<String, BigDecimal> splits = new LinkedHashMap<>();
            splits.put("SHOP_WALLET_102", netShop);
            splits.put("PLATFORM_REVENUE", BigDecimal.ZERO); // Không thu hoa hồng

            LedgerEntry entryShop = LedgerEntry.builder()
                    .id(12L)
                    .transactionId(txnId)
                    .transactionType("ESCROW_RELEASE")
                    .accountDebit("ESCROW_HOLDING")
                    .accountCredit("SHOP_WALLET_102")
                    .amount(netShop)
                    .build();

            when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(entryShop);
            when(ledgerEntryRepository.findByTransactionId(txnId)).thenReturn(List.of(entryShop));

            List<LedgerEntry> entries = ledgerService.recordSplitCreditEntry(
                    txnId,
                    "ESCROW_RELEASE",
                    "ESCROW_HOLDING",
                    splits
            );

            // Bút toán bằng 0 được bỏ qua để giữ sạch sổ cái
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getAccountCredit()).isEqualTo("SHOP_WALLET_102");
            assertThat(entries.get(0).getAmount()).isEqualByComparingTo(netShop);

            boolean invariant = ledgerService.verifyTransactionInvariant(txnId);
            assertThat(invariant).isTrue();
            verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
        }

        @Test
        @DisplayName("Split Credit quăng ngoại lệ khi danh sách phân bổ rỗng hoặc tài khoản Có trùng tài khoản Nợ")
        void testSplitCredit_InvalidSplits_ThrowsBadRequest() {
            assertThatThrownBy(() -> ledgerService.recordSplitCreditEntry(
                    "TXN-ERR-SPLIT-1",
                    "ESCROW_RELEASE",
                    "ESCROW_HOLDING",
                    Collections.emptyMap()
            )).isInstanceOf(BadRequestException.class);

            Map<String, BigDecimal> invalidSplits = Map.of("ESCROW_HOLDING", BigDecimal.valueOf(100000));
            assertThatThrownBy(() -> ledgerService.recordSplitCreditEntry(
                    "TXN-ERR-SPLIT-2",
                    "ESCROW_RELEASE",
                    "ESCROW_HOLDING",
                    invalidSplits
            )).isInstanceOf(BadRequestException.class)
              .hasMessageContaining("không được trùng nhau");
        }
    }

    @Nested
    @DisplayName("Kiểm tra cơ chế phát hiện sai phạm bất biến kế toán (Invariant Violation Detection)")
    class InvariantViolationTests {

        @Test
        @DisplayName("Phát hiện bất thường nếu bút toán có số tiền âm hoặc bằng 0 trong CSDL")
        void testVerifyInvariant_CorruptedAmount_ThrowsIllegalState() {
            String txnId = "TXN-CORRUPTED-1";
            LedgerEntry corrupted = LedgerEntry.builder()
                    .id(99L)
                    .transactionId(txnId)
                    .transactionType("TEST")
                    .accountDebit("ACC_A")
                    .accountCredit("ACC_B")
                    .amount(BigDecimal.valueOf(-100))
                    .build();

            when(ledgerEntryRepository.findByTransactionId(txnId)).thenReturn(List.of(corrupted));

            assertThatThrownBy(() -> ledgerService.verifyTransactionInvariant(txnId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("VI PHẠM BẤT BIẾN KẾ TOÁN SỔ CÁI");
        }

        @Test
        @DisplayName("Phát hiện bất thường nếu bút toán bị thiếu tài khoản Nợ hoặc Có")
        void testVerifyInvariant_MissingAccount_ThrowsIllegalState() {
            String txnId = "TXN-CORRUPTED-2";
            LedgerEntry corrupted = LedgerEntry.builder()
                    .id(100L)
                    .transactionId(txnId)
                    .transactionType("TEST")
                    .accountDebit("ACC_A")
                    .accountCredit("")
                    .amount(BigDecimal.valueOf(100000))
                    .build();

            when(ledgerEntryRepository.findByTransactionId(txnId)).thenReturn(List.of(corrupted));

            assertThatThrownBy(() -> ledgerService.verifyTransactionInvariant(txnId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("thiếu tài khoản Nợ hoặc Có");
        }
    }

    @Nested
    @DisplayName("Kiểm tra báo cáo kiểm toán sổ cái toàn hệ thống (Ledger Audit & Invariant Report)")
    class LedgerAuditTests {

        @Test
        @DisplayName("Kiểm toán toàn hệ thống thành công: Tổng Nợ = Tổng Có, Trạng thái BALANCED")
        void testAuditLedger_SystemBalanced() {
            List<LedgerEntry> allEntries = List.of(
                    // 1. Khách trả VNPay
                    LedgerEntry.builder().transactionId("T1").transactionType("PAYMENT").accountDebit("VNPAY_GATEWAY").accountCredit("ESCROW_HOLDING").amount(BigDecimal.valueOf(1000000)).build(),
                    // 2. Escrow Release: 950k shop, 50k commission
                    LedgerEntry.builder().transactionId("T2").transactionType("RELEASE").accountDebit("ESCROW_HOLDING").accountCredit("SHOP_WALLET_101").amount(BigDecimal.valueOf(950000)).build(),
                    LedgerEntry.builder().transactionId("T2").transactionType("RELEASE").accountDebit("ESCROW_HOLDING").accountCredit("PLATFORM_REVENUE").amount(BigDecimal.valueOf(50000)).build(),
                    // 3. Shop rút 500k
                    LedgerEntry.builder().transactionId("T3").transactionType("WITHDRAWAL").accountDebit("SHOP_WALLET_101").accountCredit("BANK_SETTLEMENT").amount(BigDecimal.valueOf(500000)).build()
            );

            when(ledgerEntryRepository.findAll()).thenReturn(allEntries);

            LedgerAuditReportDto audit = ledgerService.auditLedger();

            assertThat(audit).isNotNull();
            assertThat(audit.isBalanced()).isTrue();
            assertThat(audit.getStatus()).isEqualTo("BALANCED");
            assertThat(audit.getDiscrepancy()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(audit.getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(2500000));
            assertThat(audit.getTotalCredits()).isEqualByComparingTo(BigDecimal.valueOf(2500000));
            assertThat(audit.getTotalEntries()).isEqualTo(4);

            // Kiểm tra số dư ròng theo tài khoản (Net = Total Debit - Total Credit)
            Map<String, BigDecimal> balances = audit.getAccountBalances();
            // VNPAY_GATEWAY: +1,000,000
            assertThat(balances.get("VNPAY_GATEWAY")).isEqualByComparingTo(BigDecimal.valueOf(1000000));
            // ESCROW_HOLDING: -1,000,000 (credit) + 950,000 (debit) + 50,000 (debit) = 0
            assertThat(balances.get("ESCROW_HOLDING")).isEqualByComparingTo(BigDecimal.ZERO);
            // SHOP_WALLET_101: -950,000 (credit) + 500,000 (debit) = -450,000 (Liability owed to shop)
            assertThat(balances.get("SHOP_WALLET_101")).isEqualByComparingTo(BigDecimal.valueOf(-450000));
            // PLATFORM_REVENUE: -50,000 (Equity/Revenue credit)
            assertThat(balances.get("PLATFORM_REVENUE")).isEqualByComparingTo(BigDecimal.valueOf(-50000));
            // BANK_SETTLEMENT: -500,000 (Bank payout credit)
            assertThat(balances.get("BANK_SETTLEMENT")).isEqualByComparingTo(BigDecimal.valueOf(-500000));
        }

        @Test
        @DisplayName("Lấy số dư tài khoản sổ cái chính xác theo Nợ, Có và Ròng")
        void testGetAccountBalance() {
            String account = "ESCROW_HOLDING";
            when(ledgerEntryRepository.sumDebitByAccount(account)).thenReturn(BigDecimal.valueOf(2000000));
            when(ledgerEntryRepository.sumCreditByAccount(account)).thenReturn(BigDecimal.valueOf(1500000));

            AccountBalanceDto balance = ledgerService.getAccountBalance(account);

            assertThat(balance.getAccount()).isEqualTo(account);
            assertThat(balance.getTotalDebit()).isEqualByComparingTo(BigDecimal.valueOf(2000000));
            assertThat(balance.getTotalCredit()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
            assertThat(balance.getNetBalance()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        }

        @Test
        @DisplayName("Tài khoản chưa có giao dịch nào trả về 0 thay vì null")
        void testGetAccountBalance_NonExistentAccount_ReturnsZero() {
            String account = "UNKNOWN_ACCOUNT";
            when(ledgerEntryRepository.sumDebitByAccount(account)).thenReturn(null);
            when(ledgerEntryRepository.sumCreditByAccount(account)).thenReturn(null);

            AccountBalanceDto balance = ledgerService.getAccountBalance(account);

            assertThat(balance.getAccount()).isEqualTo(account);
            assertThat(balance.getTotalDebit()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(balance.getTotalCredit()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(balance.getNetBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
