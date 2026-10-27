package com.quickshop.payment.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.payment.dto.WalletResponse;
import com.quickshop.payment.dto.WithdrawalRequestDto;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.service.WalletService;
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
@RequestMapping("/api/payments/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Controller", description = "Quản lý Ví Seller, số dư khả dụng, đóng băng và rút tiền")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "Lấy thông tin số dư Ví của Shop")
    @GetMapping("/shops/{shopId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletByShopId(
            @PathVariable Long shopId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole
    ) {
        WalletResponse response = walletService.getWalletByShopId(shopId, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin ví thành công", response));
    }

    @Operation(summary = "Seller tạo yêu cầu rút tiền về tài khoản ngân hàng")
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Withdrawal>> requestWithdrawal(
            @Valid @RequestBody WithdrawalRequestDto request,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole
    ) {
        Withdrawal withdrawal = walletService.requestWithdrawal(request, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.ok("Tạo yêu cầu rút tiền thành công", withdrawal));
    }

    @Operation(summary = "Admin phê duyệt và tất toán lệnh rút tiền")
    @PostMapping("/withdrawals/{withdrawalId}/approve")
    public ResponseEntity<ApiResponse<Withdrawal>> approveWithdrawal(
            @PathVariable Long withdrawalId,
            @RequestParam(defaultValue = "1") Long adminId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole
    ) {
        Withdrawal withdrawal = walletService.approveWithdrawal(withdrawalId, adminId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.ok("Phê duyệt và chuyển khoản rút tiền thành công", withdrawal));
    }

    @Operation(summary = "Admin từ chối lệnh rút tiền (hoàn số dư về ví)")
    @PostMapping("/withdrawals/{withdrawalId}/reject")
    public ResponseEntity<ApiResponse<Withdrawal>> rejectWithdrawal(
            @PathVariable Long withdrawalId,
            @RequestParam(defaultValue = "1") Long adminId,
            @RequestParam(defaultValue = "Thông tin tài khoản không hợp lệ") String reason,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole
    ) {
        Withdrawal withdrawal = walletService.rejectWithdrawal(withdrawalId, adminId, reason, currentUserRole);
        return ResponseEntity.ok(ApiResponse.ok("Từ chối lệnh rút tiền thành công", withdrawal));
    }

    @Operation(summary = "Lấy lịch sử các lệnh rút tiền của Shop")
    @GetMapping("/shops/{shopId}/withdrawals")
    public ResponseEntity<ApiResponse<List<Withdrawal>>> getWithdrawalsByShopId(
            @PathVariable Long shopId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole
    ) {
        List<Withdrawal> list = walletService.getWithdrawalsByShopId(shopId, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.ok("Lấy lịch sử rút tiền thành công", list));
    }

    @Operation(summary = "Tra cứu chi tiết một lệnh rút tiền")
    @GetMapping("/withdrawals/{withdrawalId}")
    public ResponseEntity<ApiResponse<Withdrawal>> getWithdrawalById(
            @PathVariable Long withdrawalId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole
    ) {
        Withdrawal withdrawal = walletService.getWithdrawalById(withdrawalId, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin lệnh rút tiền thành công", withdrawal));
    }
}
