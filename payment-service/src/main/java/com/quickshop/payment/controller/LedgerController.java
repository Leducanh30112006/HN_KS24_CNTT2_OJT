package com.quickshop.payment.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.payment.dto.ReconciliationReport;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.service.DoubleEntryLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments/ledger")
@RequiredArgsConstructor
@Tag(name = "Double-Entry Ledger Controller", description = "Quản lý Sổ cái kép và Báo cáo Đối soát Tài chính")
public class LedgerController {

    private final DoubleEntryLedgerService ledgerService;

    @Operation(summary = "Tra cứu các bút toán theo Transaction ID")
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ApiResponse<List<LedgerEntry>>> getEntriesByTransactionId(@PathVariable String transactionId) {
        List<LedgerEntry> entries = ledgerService.getEntriesByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách bút toán thành công", entries));
    }

    @Operation(summary = "Chạy đối soát toàn diện hệ thống: Kiểm tra cân đối Nợ/Có và tài khoản Escrow")
    @GetMapping("/reconciliation")
    public ResponseEntity<ApiResponse<ReconciliationReport>> runReconciliation() {
        ReconciliationReport report = ledgerService.reconcileAll();
        return ResponseEntity.ok(ApiResponse.ok("Báo cáo đối soát hệ thống", report));
    }

    @Operation(summary = "Đối soát số dư Ví của Shop với phát sinh Sổ cái")
    @GetMapping("/reconciliation/shops/{shopId}")
    public ResponseEntity<ApiResponse<Boolean>> reconcileShopWallet(@PathVariable Long shopId) {
        boolean isBalanced = ledgerService.reconcileShopWallet(shopId);
        String message = isBalanced
                ? "Số dư ví khớp 100% với số liệu Sổ cái kép"
                : "CẢNH BÁO: Phát hiện sai lệch giữa số dư ví và Sổ cái kép!";
        return ResponseEntity.ok(ApiResponse.ok(message, isBalanced));
    }
}
