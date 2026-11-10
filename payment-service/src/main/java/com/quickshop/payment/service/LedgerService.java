package com.quickshop.payment.service;

import com.quickshop.payment.dto.AccountBalanceDto;
import com.quickshop.payment.dto.LedgerAuditReportDto;
import com.quickshop.payment.entity.LedgerEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface LedgerService {

    LedgerEntry recordDoubleEntry(String transactionId, String transactionType, String accountDebit, String accountCredit, BigDecimal amount);

    List<LedgerEntry> recordSplitCreditEntry(String transactionId, String transactionType, String accountDebit, Map<String, BigDecimal> creditSplits);

    boolean verifyTransactionInvariant(String transactionId);

    LedgerAuditReportDto auditLedger();

    AccountBalanceDto getAccountBalance(String account);

    List<LedgerEntry> getEntriesByTransaction(String transactionId);

    List<LedgerEntry> getEntriesByAccount(String account);

    List<LedgerEntry> getAllEntries();
}
