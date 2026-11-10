package com.quickshop.payment.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.service.LedgerService;
import com.quickshop.payment.service.PaymentService;
import com.quickshop.payment.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final LedgerService ledgerService;

    @GetMapping("/wallet/{shopId}")
    public ResponseEntity<ApiResponse<Wallet>> getShopWallet(@PathVariable Long shopId) {
        Wallet wallet = paymentService.getWalletByShopId(shopId);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @GetMapping("/escrow/{subOrderId}")
    public ResponseEntity<ApiResponse<EscrowAccount>> getEscrowStatus(@PathVariable Long subOrderId) {
        EscrowAccount escrow = paymentService.getEscrowBySubOrderId(subOrderId);
        return ResponseEntity.ok(ApiResponse.success(escrow));
    }

    @PostMapping("/escrow/hold")
    public ResponseEntity<ApiResponse<EscrowAccount>> holdEscrow(@RequestBody HoldEscrowRequest request) {
        EscrowAccount escrow = paymentService.holdEscrow(request);
        return ResponseEntity.ok(ApiResponse.success("Ký quỹ thành công", escrow));
    }

    @PostMapping("/escrow/release")
    public ResponseEntity<ApiResponse<EscrowAccount>> releaseEscrow(@RequestBody ReleaseEscrowRequest request) {
        EscrowAccount escrow = paymentService.releaseEscrow(request);
        return ResponseEntity.ok(ApiResponse.success("Giải phóng ký quỹ cho người bán thành công", escrow));
    }

    @PostMapping("/escrow/refund")
    public ResponseEntity<ApiResponse<EscrowAccount>> refundEscrow(@RequestBody RefundEscrowRequest request) {
        EscrowAccount escrow = paymentService.refundEscrow(request);
        return ResponseEntity.ok(ApiResponse.success("Hoàn tiền ký quỹ thành công", escrow));
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<ApiResponse<Withdrawal>> requestWithdrawal(@RequestBody WithdrawalRequestDto request) {
        Withdrawal withdrawal = paymentService.requestWithdrawal(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu rút tiền thành công", withdrawal));
    }

    @GetMapping("/withdrawals/shop/{shopId}")
    public ResponseEntity<ApiResponse<List<Withdrawal>>> getShopWithdrawals(@PathVariable Long shopId) {
        List<Withdrawal> list = paymentService.getWithdrawalsByShop(shopId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/withdrawals/pending")
    public ResponseEntity<ApiResponse<List<Withdrawal>>> getPendingWithdrawals() {
        List<Withdrawal> list = paymentService.getPendingWithdrawals();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PutMapping("/withdrawals/{id}/process")
    public ResponseEntity<ApiResponse<Withdrawal>> processWithdrawal(
            @PathVariable Long id,
            @RequestBody WithdrawalActionRequest request) {
        Withdrawal withdrawal = paymentService.processWithdrawal(id, request);
        return ResponseEntity.ok(ApiResponse.success("Xử lý yêu cầu rút tiền thành công", withdrawal));
    }

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<List<LedgerEntry>>> getLedger(
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String account) {
        List<LedgerEntry> entries;
        if (transactionId != null && !transactionId.isBlank()) {
            entries = ledgerService.getEntriesByTransaction(transactionId);
        } else if (account != null && !account.isBlank()) {
            entries = ledgerService.getEntriesByAccount(account);
        } else {
            entries = ledgerService.getAllEntries();
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bút toán sổ cái thành công", entries));
    }

    @GetMapping("/ledger/audit")
    public ResponseEntity<ApiResponse<LedgerAuditReportDto>> auditLedger() {
        LedgerAuditReportDto report = ledgerService.auditLedger();
        return ResponseEntity.ok(ApiResponse.success("Kiểm toán sổ cái đối soát dòng tiền thành công", report));
    }

    @GetMapping("/ledger/accounts/{account}")
    public ResponseEntity<ApiResponse<AccountBalanceDto>> getAccountBalance(@PathVariable String account) {
        AccountBalanceDto balance = ledgerService.getAccountBalance(account);
        return ResponseEntity.ok(ApiResponse.success("Lấy số dư tài khoản sổ cái thành công", balance));
    }

    @GetMapping("/ledger/verify/{transactionId}")
    public ResponseEntity<ApiResponse<Boolean>> verifyTransaction(@PathVariable String transactionId) {
        boolean isValid = ledgerService.verifyTransactionInvariant(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Xác thực tính bất biến Nợ/Có giao dịch thành công", isValid));
    }

    @PostMapping("/vnpay/create")
    public ResponseEntity<ApiResponse<VNPayPaymentResponse>> createVNPayPayment(
            @Valid @RequestBody CreateVNPayPaymentRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = VNPayUtil.getClientIp(servletRequest);
        VNPayPaymentResponse response = paymentService.createVNPayPayment(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán VNPay thành công", response));
    }

    @PostMapping("/vnpay-create")
    public ResponseEntity<ApiResponse<Map<String, String>>> createVNPayUrl(
            @RequestParam String orderId,
            @RequestParam BigDecimal amount,
            HttpServletRequest servletRequest
    ) {
        String clientIp = VNPayUtil.getClientIp(servletRequest);
        CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                .orderId(orderId)
                .amount(amount)
                .orderInfo("Thanh toan don hang #" + orderId)
                .build();
        VNPayPaymentResponse vnpResponse = paymentService.createVNPayPayment(request, clientIp);
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", vnpResponse.getPaymentUrl());
        response.put("orderId", orderId);
        response.put("txnRef", vnpResponse.getTxnRef());
        return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán VNPay thành công", response));
    }

    @GetMapping("/transactions/{txnRef}")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> getTransactionByTxnRef(
            @PathVariable String txnRef
    ) {
        PaymentTransactionResponse response = paymentService.getTransactionByTxnRef(txnRef);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giao dịch thành công", response));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> getTransactionByOrderId(
            @PathVariable String orderId
    ) {
        PaymentTransactionResponse response = paymentService.getTransactionByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("Lấy giao dịch thanh toán của đơn hàng thành công", response));
    }

    /**
     * VNPay IPN (Instant Payment Notification) Webhook
     * VNPay Server gọi sang endpoint này sau khi khách hoàn tất giao dịch
     */
    @GetMapping({"/vnpay/ipn", "/vnpay-ipn"})
    public ResponseEntity<Map<String, String>> processVNPayIpn(@RequestParam Map<String, String> allParams) {
        Map<String, String> result = paymentService.processVNPayIpn(allParams);
        return ResponseEntity.ok(result);
    }

    /**
     * VNPay Return URL: Khách hàng được redirect về sau khi thanh toán trên cổng VNPay
     */
    @GetMapping({"/vnpay/return", "/vnpay-return"})
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> processVNPayReturn(@RequestParam Map<String, String> allParams) {
        PaymentTransactionResponse response = paymentService.processVNPayReturn(allParams);
        String msg = (response.getStatus() == PaymentStatus.SUCCESS)
                ? "Thanh toán đơn hàng qua VNPay thành công"
                : "Giao dịch thanh toán VNPay không thành công hoặc đã bị hủy";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    /**
     * Kiểm tra trạng thái thanh toán theo đơn hàng (dùng cho polling thời gian thực)
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPaymentStatus(@PathVariable String orderId) {
        Map<String, Object> status = paymentService.checkPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra trạng thái thanh toán thành công", status));
    }

    /**
     * Webhook tiếp nhận thông báo biến động số dư từ PayOS khi khách chuyển khoản vào MB Bank
     */
    @PostMapping("/payos/webhook")
    public ResponseEntity<Map<String, Object>> handlePayOsWebhook(@RequestBody Map<String, Object> webhookData) {
        Map<String, Object> response = paymentService.processPayOsWebhook(webhookData);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint giả lập chuyển khoản MB Bank thành công (dùng cho dev/test và demo kiểm thử tức thì)
     */
    @PostMapping("/payos/simulate-success")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> simulateSuccess(
            @RequestParam String orderId,
            @RequestParam(required = false) BigDecimal amount
    ) {
        PaymentTransactionResponse response = paymentService.simulatePaymentSuccess(orderId, amount);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận chuyển khoản MB Bank thành công", response));
    }
}
