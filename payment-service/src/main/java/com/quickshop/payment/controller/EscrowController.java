package com.quickshop.payment.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.service.EscrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments/escrow")
@RequiredArgsConstructor
@Tag(name = "Escrow Controller", description = "Quản lý tài khoản tạm giữ (Escrow), giải phóng tiền và đóng băng tranh chấp")
public class EscrowController {

    private final EscrowService escrowService;

    @Operation(summary = "Tạm giữ tiền (Escrow Hold) cho một Sub-Order")
    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<EscrowResponse>> holdEscrow(@Valid @RequestBody HoldEscrowRequest request) {
        EscrowResponse response = escrowService.holdEscrow(request);
        return ResponseEntity.ok(ApiResponse.ok("Tạm giữ tiền Escrow thành công", response));
    }

    @Operation(summary = "Giải phóng Escrow cho Seller (Buyer xác nhận hoặc đơn DELIVERED quá 3 ngày)")
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<EscrowResponse>> releaseEscrow(@Valid @RequestBody ReleaseEscrowRequest request) {
        EscrowResponse response = escrowService.releaseEscrow(request);
        return ResponseEntity.ok(ApiResponse.ok("Giải phóng tiền Escrow cho Seller thành công", response));
    }

    @Operation(summary = "Hoàn tiền Escrow lại cho Buyer do đơn hủy hoặc khiếu nại thành công")
    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<EscrowResponse>> refundEscrow(@Valid @RequestBody RefundEscrowRequest request) {
        EscrowResponse response = escrowService.refundEscrow(request);
        return ResponseEntity.ok(ApiResponse.ok("Hoàn tiền Escrow cho khách hàng thành công", response));
    }

    @Operation(summary = "Đóng băng giải phóng Escrow khi Sub-Order có tranh chấp / khiếu nại")
    @PostMapping("/dispute")
    public ResponseEntity<ApiResponse<EscrowResponse>> freezeEscrowForDispute(@Valid @RequestBody DisputeEscrowRequest request) {
        EscrowResponse response = escrowService.freezeEscrowForDispute(request.getSubOrderId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.ok("Đã đóng băng giải phóng Escrow do có tranh chấp", response));
    }

    @Operation(summary = "Mở đóng băng Escrow khi khiếu nại tranh chấp bị bác bỏ")
    @PostMapping("/sub-orders/{subOrderId}/unfreeze")
    public ResponseEntity<ApiResponse<EscrowResponse>> unfreezeEscrow(@PathVariable Long subOrderId) {
        EscrowResponse response = escrowService.unfreezeEscrow(subOrderId);
        return ResponseEntity.ok(ApiResponse.ok("Mở đóng băng Escrow thành công", response));
    }

    @Operation(summary = "Tra cứu tài khoản Escrow theo Sub-Order ID")
    @GetMapping("/sub-orders/{subOrderId}")
    public ResponseEntity<ApiResponse<EscrowResponse>> getEscrowBySubOrderId(@PathVariable Long subOrderId) {
        EscrowResponse response = escrowService.getEscrowBySubOrderId(subOrderId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin Escrow thành công", response));
    }

    @Operation(summary = "Tra cứu danh sách Escrow theo Master Order ID")
    @GetMapping("/master-orders/{masterOrderId}")
    public ResponseEntity<ApiResponse<List<EscrowResponse>>> getEscrowsByMasterOrderId(@PathVariable Long masterOrderId) {
        List<EscrowResponse> list = escrowService.getEscrowsByMasterOrderId(masterOrderId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách Escrow theo Master Order thành công", list));
    }

    @Operation(summary = "Tác vụ định kỳ: Quét và giải phóng các Escrow DELIVERED quá 3 ngày không có khiếu nại")
    @PostMapping("/auto-release")
    public ResponseEntity<ApiResponse<Integer>> triggerAutoRelease() {
        int releasedCount = escrowService.autoReleaseEligibleEscrows();
        return ResponseEntity.ok(ApiResponse.ok("Đã tự động giải phóng " + releasedCount + " tài khoản Escrow", releasedCount));
    }
}
