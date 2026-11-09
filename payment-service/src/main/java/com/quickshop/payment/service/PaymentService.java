package com.quickshop.payment.service;

import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;

import java.util.List;

public interface PaymentService {
    Wallet getWalletByShopId(Long shopId);
    EscrowAccount getEscrowBySubOrderId(Long subOrderId);
    EscrowAccount holdEscrow(HoldEscrowRequest request);
    EscrowAccount releaseEscrow(ReleaseEscrowRequest request);
    EscrowAccount refundEscrow(RefundEscrowRequest request);
    Withdrawal requestWithdrawal(WithdrawalRequestDto request);
    Withdrawal processWithdrawal(Long withdrawalId, WithdrawalActionRequest request);
    List<Withdrawal> getWithdrawalsByShop(Long shopId);
    List<Withdrawal> getPendingWithdrawals();
    List<LedgerEntry> getAllLedgerEntries();
    List<LedgerEntry> getLedgerEntriesByTxn(String transactionId);
}
