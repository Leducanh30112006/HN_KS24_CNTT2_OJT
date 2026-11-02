package com.quickshop.payment.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final WalletRepository walletRepository;
    private final EscrowAccountRepository escrowAccountRepository;

    @Value("${vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpayPaymentUrl;

    @GetMapping("/wallet/{shopId}")
    public ResponseEntity<ApiResponse<Wallet>> getShopWallet(@PathVariable Long shopId) {
        Wallet wallet = walletRepository.findByShopId(shopId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .shopId(shopId)
                        .balance(BigDecimal.ZERO)
                        .frozenBalance(BigDecimal.ZERO)
                        .build()));
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @GetMapping("/escrow/{subOrderId}")
    public ResponseEntity<ApiResponse<EscrowAccount>> getEscrowStatus(@PathVariable Long subOrderId) {
        Optional<EscrowAccount> escrowOpt = escrowAccountRepository.findBySubOrderId(subOrderId);
        if (escrowOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(escrowOpt.get()));
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
