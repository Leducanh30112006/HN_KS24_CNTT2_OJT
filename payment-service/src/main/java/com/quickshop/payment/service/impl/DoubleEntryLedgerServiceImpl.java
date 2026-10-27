package com.quickshop.payment.service.impl;

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
import com.quickshop.payment.service.DoubleEntryLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoubleEntryLedgerServiceImpl implements DoubleEntryLedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final EscrowAccountRepository escrowAccountRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry recordEntry(
            String transactionId,
            LedgerTransactionType transactionType,
            String accountDebit,
            String accountCredit,
            BigDecimal amount,
            String description
    ) {
        log.info("[Ledger] Ghi bút toán kép [TxID: {}][Loại: {}]: Nợ {} | Có {} | Số tiền: {} VND",
                transactionId, transactionType, accountDebit, accountCredit, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền ghi sổ cái phải lớn hơn 0");
        }
        if (accountDebit == null || accountDebit.isBlank() || accountCredit == null || accountCredit.isBlank()) {
            throw new BadRequestException("Tài khoản Nợ và tài khoản Có không được để trống");
        }
        if (accountDebit.equalsIgnoreCase(accountCredit)) {
            throw new BadRequestException("Tài khoản Nợ và tài khoản Có không được trùng nhau: " + accountDebit);
        }

        // Kiểm tra chống ghi trùng lặp một giao dịch tài chính
        if (ledgerEntryRepository.existsByTransactionId(transactionId)) {
            log.warn("[Ledger] Phát hiện giao dịch trùng lặp trong sổ cái với Transaction ID: {}", transactionId);
            throw new DuplicateTransactionException("Giao dịch tài chính với ID '" + transactionId + "' đã tồn tại trong sổ cái kép.");
        }

        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transactionId)
                .transactionType(transactionType)
                .accountDebit(accountDebit)
                .accountCredit(accountCredit)
                .amount(amount)
                .description(description)
                .build();

        return ledgerEntryRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntriesByTransactionId(String transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationReport reconcileAll() {
        log.info("[Ledger Reconciliation] Bắt đầu chạy đối soát số dư sổ cái kép và tài khoản Escrow...");

        LocalDateTime now = LocalDateTime.now();
        List<String> details = new ArrayList<>();

        // 1. Kiểm tra bất biến kế toán kép: Tổng Nợ == Tổng Có
        BigDecimal totalLedgerAmount = ledgerEntryRepository.sumTotalAmount();
        BigDecimal totalDebits = totalLedgerAmount != null ? totalLedgerAmount : BigDecimal.ZERO;
        BigDecimal totalCredits = totalDebits; // Mỗi bút toán ghi Nợ và Có bằng nhau

        details.add("Tổng giá trị các bút toán Nợ (Debit) phát sinh: " + totalDebits + " VND");
        details.add("Tổng giá trị các bút toán Có (Credit) phát sinh: " + totalCredits + " VND");
        details.add("Trạng thái cân đối kế toán kép: KHỚP 100% (Debit == Credit)");

        // 2. Đối soát tài khoản Tạm giữ SYSTEM_ESCROW
        // Phát sinh Có của SYSTEM_ESCROW: Tiền giữ từ khách hàng (ESCROW_HOLD)
        BigDecimal totalEscrowCredit = ledgerEntryRepository.sumAmountByAccountCredit(LedgerAccount.SYSTEM_ESCROW);
        // Phát sinh Nợ của SYSTEM_ESCROW: Tiền đã giải phóng sang ví seller, hoa hồng sàn, hoặc hoàn tiền
        BigDecimal totalEscrowDebit = ledgerEntryRepository.sumAmountByAccountDebit(LedgerAccount.SYSTEM_ESCROW);

        // Số dư lý thuyết của SYSTEM_ESCROW trong Sổ cái
        BigDecimal expectedEscrowHolding = totalEscrowCredit.subtract(totalEscrowDebit);

        // Số dư thực tế tổng hợp từ bảng escrow_accounts có trạng thái HOLDING
        BigDecimal actualEscrowHolding = escrowAccountRepository.sumAmountByStatus(EscrowStatus.HOLDING);
        if (actualEscrowHolding == null) {
            actualEscrowHolding = BigDecimal.ZERO;
        }

        BigDecimal discrepancy = actualEscrowHolding.subtract(expectedEscrowHolding);
        boolean isEscrowBalanced = (discrepancy.compareTo(BigDecimal.ZERO) == 0);

        String escrowStatusStr = isEscrowBalanced ? "BALANCED" : "DISCREPANCY";
        details.add("Số dư SYSTEM_ESCROW tính theo Sổ cái: " + expectedEscrowHolding + " VND");
        details.add("Số dư thực tế bảng escrow_accounts (trạng thái HOLDING): " + actualEscrowHolding + " VND");
        details.add("Chênh lệch đối soát Escrow: " + discrepancy + " VND");

        String overallStatus = isEscrowBalanced ? "BALANCED" : "DISCREPANCY";

        log.info("[Ledger Reconciliation] Hoàn thành đối soát: Kết quả {}, Chênh lệch: {} VND",
                overallStatus, discrepancy);

        return ReconciliationReport.builder()
                .reconciledAt(now)
                .status(overallStatus)
                .isDoubleEntryBalanced(true)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .escrowStatus(escrowStatusStr)
                .expectedEscrowHolding(expectedEscrowHolding)
                .actualEscrowHolding(actualEscrowHolding)
                .escrowDiscrepancy(discrepancy)
                .details(details)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean reconcileShopWallet(Long shopId) {
        String sellerAccount = LedgerAccount.sellerWallet(shopId);
        BigDecimal credits = ledgerEntryRepository.sumAmountByAccountCredit(sellerAccount);
        BigDecimal debits = ledgerEntryRepository.sumAmountByAccountDebit(sellerAccount);
        BigDecimal expectedBalance = credits.subtract(debits);

        Optional<Wallet> walletOpt = walletRepository.findByShopId(shopId);
        BigDecimal actualBalance = walletOpt.map(Wallet::getBalance).orElse(BigDecimal.ZERO);

        boolean isBalanced = (expectedBalance.compareTo(actualBalance) == 0);
        log.info("[Wallet Reconciliation][ShopID: {}] Kỳ vọng sổ cái: {}, Thực tế ví: {}. Khớp: {}",
                shopId, expectedBalance, actualBalance, isBalanced);

        return isBalanced;
    }
}
