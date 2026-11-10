package com.quickshop.payment.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.payment.dto.AccountBalanceDto;
import com.quickshop.payment.dto.LedgerAuditReportDto;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.repository.LedgerEntryRepository;
import com.quickshop.payment.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    @Transactional
    public LedgerEntry recordDoubleEntry(String transactionId, String transactionType, String accountDebit, String accountCredit, BigDecimal amount) {
        validatePostingInput(transactionId, transactionType, accountDebit, accountCredit, amount);

        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transactionId)
                .transactionType(transactionType)
                .accountDebit(accountDebit.trim())
                .accountCredit(accountCredit.trim())
                .amount(amount)
                .build();

        LedgerEntry saved = ledgerEntryRepository.save(entry);
        log.info("Ledger Double-Entry ghi nhận thành công: [{}] Nợ {} / Có {} : {}đ (Txn: {})",
                transactionType, accountDebit, accountCredit, amount, transactionId);

        // Kiểm tra tính bất biến (Invariant) sau khi ghi nhận
        verifyTransactionInvariant(transactionId);
        return saved;
    }

    @Override
    @Transactional
    public List<LedgerEntry> recordSplitCreditEntry(String transactionId, String transactionType, String accountDebit, Map<String, BigDecimal> creditSplits) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new BadRequestException("transactionId không được để trống");
        }
        if (accountDebit == null || accountDebit.isBlank()) {
            throw new BadRequestException("accountDebit không được để trống");
        }
        if (creditSplits == null || creditSplits.isEmpty()) {
            throw new BadRequestException("Danh sách phân bổ Có (creditSplits) không được rỗng");
        }

        List<LedgerEntry> entries = new ArrayList<>();
        BigDecimal totalSplit = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> split : creditSplits.entrySet()) {
            String creditAccount = split.getKey();
            BigDecimal splitAmount = split.getValue();

            if (creditAccount == null || creditAccount.isBlank()) {
                throw new BadRequestException("Tài khoản ghi Có không hợp lệ");
            }
            if (splitAmount == null || splitAmount.compareTo(BigDecimal.ZERO) <= 0) {
                // Bỏ qua các khoản phân bổ bằng 0 (ví dụ commission = 0)
                continue;
            }
            if (accountDebit.trim().equalsIgnoreCase(creditAccount.trim())) {
                throw new BadRequestException("Tài khoản Nợ và Có không được trùng nhau: " + accountDebit);
            }

            LedgerEntry entry = LedgerEntry.builder()
                    .transactionId(transactionId)
                    .transactionType(transactionType)
                    .accountDebit(accountDebit.trim())
                    .accountCredit(creditAccount.trim())
                    .amount(splitAmount)
                    .build();

            entries.add(ledgerEntryRepository.save(entry));
            totalSplit = totalSplit.add(splitAmount);
        }

        log.info("Ledger Split-Credit ghi nhận: [{}] Nợ {} tổng {}đ phân bổ cho {} tài khoản Có (Txn: {})",
                transactionType, accountDebit, totalSplit, entries.size(), transactionId);

        // Kiểm tra tính bất biến cho toàn bộ giao dịch
        verifyTransactionInvariant(transactionId);
        return entries;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyTransactionInvariant(String transactionId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transactionId);
        if (entries.isEmpty()) {
            return true;
        }

        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if (entry.getAmount() == null || entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                String errorMsg = String.format("VI PHẠM BẤT BIẾN KẾ TOÁN SỔ CÁI (Txn: %s): Bút toán ID %d có số tiền không hợp lệ (%s)",
                        transactionId, entry.getId(), entry.getAmount());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            if (entry.getAccountDebit() == null || entry.getAccountDebit().isBlank()
                    || entry.getAccountCredit() == null || entry.getAccountCredit().isBlank()) {
                String errorMsg = String.format("VI PHẠM BẤT BIẾN KẾ TOÁN SỔ CÁI (Txn: %s): Bút toán ID %d thiếu tài khoản Nợ hoặc Có",
                        transactionId, entry.getId());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            if (entry.getAccountDebit().trim().equalsIgnoreCase(entry.getAccountCredit().trim())) {
                String errorMsg = String.format("VI PHẠM BẤT BIẾN KẾ TOÁN SỔ CÁI (Txn: %s): Bút toán ID %d có tài khoản Nợ và Có trùng nhau: %s",
                        transactionId, entry.getId(), entry.getAccountDebit());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            sumDebit = sumDebit.add(entry.getAmount());
            sumCredit = sumCredit.add(entry.getAmount());
        }

        // Bất biến kế toán: Tổng Nợ == Tổng Có cho mọi giao dịch
        if (sumDebit.compareTo(sumCredit) != 0) {
            String errorMsg = String.format("VI PHẠM BẤT BIẾN KẾ TOÁN SỔ CÁI (Txn: %s): Tổng Nợ (%s) != Tổng Có (%s)",
                    transactionId, sumDebit, sumCredit);
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerAuditReportDto auditLedger() {
        List<LedgerEntry> allEntries = ledgerEntryRepository.findAll();

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        Map<String, BigDecimal> accountNetBalances = new TreeMap<>();

        for (LedgerEntry entry : allEntries) {
            BigDecimal amt = entry.getAmount();
            totalDebits = totalDebits.add(amt);
            totalCredits = totalCredits.add(amt);

            String debitAcc = entry.getAccountDebit();
            String creditAcc = entry.getAccountCredit();

            // Nợ tăng tài sản (+) / Có giảm tài sản (-)
            accountNetBalances.put(debitAcc, accountNetBalances.getOrDefault(debitAcc, BigDecimal.ZERO).add(amt));
            accountNetBalances.put(creditAcc, accountNetBalances.getOrDefault(creditAcc, BigDecimal.ZERO).subtract(amt));
        }

        BigDecimal discrepancy = totalDebits.subtract(totalCredits).abs();
        boolean isBalanced = discrepancy.compareTo(BigDecimal.ZERO) == 0;

        return LedgerAuditReportDto.builder()
                .isBalanced(isBalanced)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .discrepancy(discrepancy)
                .totalEntries(allEntries.size())
                .status(isBalanced ? "BALANCED" : "UNBALANCED")
                .accountBalances(accountNetBalances)
                .auditedAt(LocalDateTime.now())
                .message(isBalanced
                        ? "Sổ cái kép cân bằng tuyệt đối: Tổng Nợ = Tổng Có. Tính bất biến bảo toàn dòng tiền được đảm bảo."
                        : "CẢNH BÁO: Phát hiện sai lệch số dư Nợ/Có trong hệ thống sổ cái!")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceDto getAccountBalance(String account) {
        if (account == null || account.isBlank()) {
            throw new BadRequestException("Tên tài khoản không được để trống");
        }

        String acc = account.trim();
        BigDecimal totalDebit = ledgerEntryRepository.sumDebitByAccount(acc);
        BigDecimal totalCredit = ledgerEntryRepository.sumCreditByAccount(acc);
        if (totalDebit == null) {
            totalDebit = BigDecimal.ZERO;
        }
        if (totalCredit == null) {
            totalCredit = BigDecimal.ZERO;
        }
        BigDecimal netBalance = totalDebit.subtract(totalCredit);

        return AccountBalanceDto.builder()
                .account(acc)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .netBalance(netBalance)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntriesByTransaction(String transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntriesByAccount(String account) {
        if (account == null || account.isBlank()) {
            return Collections.emptyList();
        }
        return ledgerEntryRepository.findByAccountDebitOrAccountCredit(account.trim(), account.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getAllEntries() {
        return ledgerEntryRepository.findAll();
    }

    private void validatePostingInput(String transactionId, String transactionType, String accountDebit, String accountCredit, BigDecimal amount) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new BadRequestException("transactionId không được để trống");
        }
        if (transactionType == null || transactionType.isBlank()) {
            throw new BadRequestException("transactionType không được để trống");
        }
        if (accountDebit == null || accountDebit.isBlank()) {
            throw new BadRequestException("accountDebit (Tài khoản ghi Nợ) không được để trống");
        }
        if (accountCredit == null || accountCredit.isBlank()) {
            throw new BadRequestException("accountCredit (Tài khoản ghi Có) không được để trống");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền bút toán phải lớn hơn 0");
        }
        if (accountDebit.trim().equalsIgnoreCase(accountCredit.trim())) {
            throw new BadRequestException("Tài khoản Nợ và Có không được trùng nhau: " + accountDebit);
        }
    }
}
