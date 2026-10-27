package com.quickshop.payment.service;

import com.quickshop.payment.dto.ReconciliationReport;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.enums.LedgerTransactionType;

import java.math.BigDecimal;
import java.util.List;

public interface DoubleEntryLedgerService {

    /**
     * Ghi một bút toán kép vào sổ cái chung.
     * Đảm bảo tính Idempotent: Không ghi trùng lặp một mã giao dịch tài chính.
     */
    LedgerEntry recordEntry(
            String transactionId,
            LedgerTransactionType transactionType,
            String accountDebit,
            String accountCredit,
            BigDecimal amount,
            String description
    );

    /**
     * Lấy danh sách các bút toán theo mã giao dịch
     */
    List<LedgerEntry> getEntriesByTransactionId(String transactionId);

    /**
     * Thực hiện đối soát toàn diện hệ thống:
     * 1. Tổng Nợ (Debit) == Tổng Có (Credit).
     * 2. Đối soát số dư SYSTEM_ESCROW trong Sổ cái với thực tế các tài khoản Escrow đang HOLDING.
     */
    ReconciliationReport reconcileAll();

    /**
     * Đối soát riêng cho ví của một Shop: So sánh số dư ví với phát sinh Sổ cái (Credit - Debit).
     */
    boolean reconcileShopWallet(Long shopId);
}
