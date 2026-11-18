package com.quickshop.payment.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.service.LedgerService;
import com.quickshop.payment.service.PayOsService;
import com.quickshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PayOsService payOsService;
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
    public ResponseEntity<ApiResponse<Withdrawal>> requestWithdrawal(@RequestBody(required = false) WithdrawalRequestDto request) {
        if (request == null || request.getShopId() == null) {
            throw new BadRequestException("Thiếu thông tin gian hàng (shopId) hoặc số tiền rút");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền rút phải lớn hơn 0");
        }
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

    // =========================================================================
    // CỔNG THANH TOÁN CHUYỂN KHOẢN PAYOS (MB BANK 24/7)
    // =========================================================================

    /**
     * Tạo yêu cầu thanh toán PayOS chuyển khoản MB Bank chỉ cần nhận orderId.
     * Tự động tra cứu số tiền đơn hàng, tự sinh request và đóng gói response đầy đủ.
     * Hỗ trợ nhận orderId qua Query Param (?orderId=...), Path Variable (/payos/create/{orderId}),
     * hoặc JSON Request Body ({ "orderId": "..." }).
     */
    @PostMapping({"/create", "/payos/create", "/payos/create/{orderId}"})
    public ResponseEntity<ApiResponse<CreatePayOsPaymentResponse>> createPayment(
            @PathVariable(required = false) String orderId,
            @RequestParam(required = false, name = "orderId") String orderIdQuery,
            @RequestParam(required = false, name = "orderIdParam") String orderIdParam,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String targetOrderId = orderId;
        if (targetOrderId == null || targetOrderId.isBlank()) {
            targetOrderId = orderIdQuery;
        }
        if (targetOrderId == null || targetOrderId.isBlank()) {
            targetOrderId = orderIdParam;
        }
        if ((targetOrderId == null || targetOrderId.isBlank()) && body != null && body.get("orderId") != null) {
            targetOrderId = String.valueOf(body.get("orderId"));
        }
        if (targetOrderId == null || targetOrderId.isBlank() || "null".equalsIgnoreCase(targetOrderId.trim())) {
            throw new BadRequestException("Mã đơn hàng (orderId) không được để trống");
        }

        BigDecimal customAmount = null;
        if (body != null && body.containsKey("amount") && body.get("amount") != null) {
            try {
                customAmount = new BigDecimal(String.valueOf(body.get("amount")));
            } catch (Exception ex) {
                throw new BadRequestException("Số tiền thanh toán (amount) không đúng định dạng số");
            }
            if (customAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Số tiền thanh toán (amount) phải lớn hơn 0");
            }
        }

        CreatePayOsPaymentResponse response = (customAmount != null)
                ? payOsService.createPayment(targetOrderId.trim(), customAmount)
                : payOsService.createPayment(targetOrderId.trim());

        return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu thanh toán PayOS thành công", response));
    }

    /**
     * Kiểm tra trạng thái thanh toán xem đã chuyển tiền chưa (Active Polling qua PayOS API)
     */
    @GetMapping({"/status/{orderId}", "/payos/status/{orderId}"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPaymentStatus(@PathVariable String orderId) {
        Map<String, Object> status = payOsService.checkPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra trạng thái thanh toán thành công", status));
    }

    /**
     * Webhook tiếp nhận thông báo biến động số dư từ PayOS khi khách chuyển khoản vào MB Bank
     */
    @PostMapping("/payos/webhook")
    public ResponseEntity<Map<String, Object>> handlePayOsWebhook(@RequestBody Map<String, Object> webhookData) {
        Map<String, Object> response = payOsService.processWebhook(webhookData);
        return ResponseEntity.ok(response);
    }

    /**
     * Xác nhận và đăng ký Webhook URL với PayOS server
     */
    @PostMapping("/payos/confirm-webhook")
    public ResponseEntity<ApiResponse<String>> confirmPayOsWebhook(
            @RequestParam(required = false) String webhookUrl,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String targetUrl = webhookUrl;
        if ((targetUrl == null || targetUrl.isBlank()) && body != null) {
            targetUrl = body.getOrDefault("webhookUrl", "");
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new BadRequestException("Webhook URL không được để trống");
        }
        String result = payOsService.confirmWebhook(targetUrl);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận Webhook PayOS thành công", result));
    }

    /**
     * Hủy liên kết thanh toán PayOS
     */
    @PostMapping("/payos/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelPayOsPayment(
            @RequestParam String orderId,
            @RequestParam(required = false, defaultValue = "Khách hàng hủy thanh toán") String reason
    ) {
        Map<String, Object> result = payOsService.cancelPaymentLink(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy liên kết thanh toán PayOS thành công", result));
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
     * Endpoint giả lập chuyển khoản MB Bank thành công (chỉ cho phép ROLE_ADMIN xác nhận thủ công)
     */
    @PostMapping("/payos/simulate-success")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> simulateSuccess(
            @RequestParam String orderId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (userRole == null || (!"ROLE_ADMIN".equalsIgnoreCase(userRole.trim()) && !"ADMIN".equalsIgnoreCase(userRole.trim()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Endpoint xác nhận thanh toán thủ công chỉ dành cho Quản trị viên (ROLE_ADMIN)"));
        }
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền xác nhận phải lớn hơn 0");
        }
        PaymentTransactionResponse response = paymentService.simulatePaymentSuccess(orderId, amount);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận chuyển khoản MB Bank thành công", response));
    }
}
