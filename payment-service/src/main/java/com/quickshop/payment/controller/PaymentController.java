package com.quickshop.payment.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.payment.dto.VNPayCreatePaymentRequest;
import com.quickshop.payment.dto.VNPayIpnResponse;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.dto.VNPayReturnResponse;
import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment & VNPay Controller", description = "Quản lý cổng thanh toán VNPay Sandbox, IPN Webhook và Return URL")
public class PaymentController {

    private final VNPayService vnPayService;

    @Operation(summary = "Tạo URL thanh toán VNPay Sandbox")
    @PostMapping("/vnpay/create")
    public ResponseEntity<ApiResponse<VNPayPaymentResponse>> createPaymentUrl(
            @Valid @RequestBody VNPayCreatePaymentRequest request,
            HttpServletRequest servletRequest
    ) {
        VNPayPaymentResponse response = vnPayService.createPaymentUrl(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.ok("Tạo URL thanh toán thành công", response));
    }

    @Operation(summary = "IPN Webhook từ cổng thanh toán VNPay (Server-to-Server)")
    @RequestMapping(value = "/vnpay/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<VNPayIpnResponse> handleVNPayIpn(@RequestParam Map<String, String> allParams) {
        log.info("[PaymentController] Nhận IPN Webhook với các tham số: {}", allParams);
        VNPayIpnResponse response = vnPayService.processIpn(allParams);
        // VNPay yêu cầu luôn trả về HTTP 200 OK kèm mã RspCode dạng JSON phẳng
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Return URL khi khách hàng được redirect về từ VNPay")
    @GetMapping("/vnpay/return")
    public ResponseEntity<ApiResponse<VNPayReturnResponse>> handleVNPayReturn(
            @RequestParam Map<String, String> allParams
    ) {
        log.info("[PaymentController] Nhận redirect Return URL với các tham số: {}", allParams);
        VNPayReturnResponse response = vnPayService.processReturn(allParams);
        return ResponseEntity.ok(ApiResponse.ok("Kết quả xử lý Return URL", response));
    }

    @Operation(summary = "Tra cứu giao dịch thanh toán mới nhất theo Order ID")
    @GetMapping("/orders/{orderId}/transaction")
    public ResponseEntity<ApiResponse<PaymentTransaction>> getLatestTransaction(@PathVariable Long orderId) {
        PaymentTransaction transaction = vnPayService.getLatestTransactionByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin giao dịch thành công", transaction));
    }

    @Operation(summary = "Tra cứu giao dịch thanh toán theo TxnRef")
    @GetMapping("/transactions/{txnRef}")
    public ResponseEntity<ApiResponse<PaymentTransaction>> getTransactionByTxnRef(@PathVariable String txnRef) {
        PaymentTransaction transaction = vnPayService.getTransactionByTxnRef(txnRef);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin giao dịch thành công", transaction));
    }
}
