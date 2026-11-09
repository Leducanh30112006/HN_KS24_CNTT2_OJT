package com.quickshop.payment.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpayPaymentUrl;

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
            @RequestParam(required = false) String transactionId) {
        List<LedgerEntry> entries = (transactionId != null && !transactionId.isBlank())
                ? paymentService.getLedgerEntriesByTxn(transactionId)
                : paymentService.getAllLedgerEntries();
        return ResponseEntity.ok(ApiResponse.success("Lấy sổ cái thành công", entries));
    }

    @PostMapping("/vnpay-create")
    public ResponseEntity<ApiResponse<Map<String, String>>> createVNPayUrl(
            @RequestParam String orderId,
            @RequestParam BigDecimal amount
    ) {
        String mockPaymentUrl = vnpayPaymentUrl + "?vnp_Amount=" + amount.multiply(new BigDecimal(100)) + "&vnp_TxnRef=" + orderId;
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", mockPaymentUrl);
        response.put("orderId", orderId);
        return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán VNPay thành công", response));
    }
}
