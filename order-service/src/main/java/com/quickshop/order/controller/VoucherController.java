package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.order.dto.voucher.ApplyVoucherResult;
import com.quickshop.order.dto.voucher.ValidateVoucherRequest;
import com.quickshop.order.dto.voucher.VoucherResponse;
import com.quickshop.order.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@Tag(name = "Voucher Management", description = "APIs tra cứu và xác thực Voucher kép (Toàn sàn & Shop)")
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "Tra cứu chi tiết voucher theo mã code")
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucher(@PathVariable String code) {
        VoucherResponse voucher = voucherService.getVoucherByCode(code);
        return ResponseEntity.ok(ApiResponse.ok("Tra cứu voucher thành công", voucher));
    }

    @Operation(summary = "Xem trước (preview) mức giảm giá của voucher trên đơn hàng")
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<ApplyVoucherResult>> previewVoucher(
            @Valid @RequestBody ValidateVoucherRequest request) {

        ApplyVoucherResult result = voucherService.previewVoucherDiscount(
                request.getCode(),
                request.getShopId(),
                request.getOrderAmount()
        );
        return ResponseEntity.ok(ApiResponse.ok("Xem trước giảm giá voucher thành công", result));
    }
}
