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

    VNPayPaymentResponse createVNPayPayment(CreateVNPayPaymentRequest request, String clientIp);
    PaymentTransactionResponse getTransactionByTxnRef(String txnRef);
    PaymentTransactionResponse getTransactionByOrderId(String orderId);
    java.util.Map<String, String> processVNPayIpn(java.util.Map<String, String> vnpParams);
    PaymentTransactionResponse processVNPayReturn(java.util.Map<String, String> vnpParams);

    java.util.Map<String, Object> checkPaymentStatus(String orderId);
    CreatePayOsPaymentResponse createPayOsPayment(String orderId);
    CreatePayOsPaymentResponse createPayOsPayment(String orderId, java.math.BigDecimal amount);
    CreatePayOsPaymentResponse createPayOsPayment(CreatePayOsPaymentRequest request);
    java.util.Map<String, Object> processPayOsWebhook(java.util.Map<String, Object> webhookData);
    String confirmPayOsWebhook(String webhookUrl);
    java.util.Map<String, Object> cancelPayOsPayment(String orderId, String reason);
    PaymentTransactionResponse simulatePaymentSuccess(String orderId, java.math.BigDecimal amount);
}
