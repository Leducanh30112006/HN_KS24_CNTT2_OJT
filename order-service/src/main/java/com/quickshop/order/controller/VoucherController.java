package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.order.dto.voucher.*;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.repository.VoucherRepository;
import com.quickshop.order.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@Tag(name = "Voucher & Promotion API", description = "Các điểm cuối quản lý, xác thực và tính toán chiết khấu Voucher sàn và Voucher Shop")
public class VoucherController {

    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService;

    @Operation(summary = "Kiểm tra tính hợp lệ và tính mức giảm giá của một Voucher cụ thể")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<VoucherValidationResponse>> validateVoucher(
            @Valid @RequestBody VoucherValidationRequest request
    ) {
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                request.getCode(), request.getShopId(), request.getOrderSubtotal());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Tính toán chiết khấu cho cả combo Voucher sàn và Voucher từng Shop (Voucher Kép)")
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<VoucherApplyResponse>> applyVouchers(
            @RequestBody VoucherApplyRequest request
    ) {
        VoucherApplyResponse response = voucherService.calculateDiscounts(request);
        return ResponseEntity.ok(ApiResponse.success("Tính toán chiết khấu voucher thành công", response));
    }

    @Operation(summary = "Lấy danh sách các voucher khả dụng cho giỏ hàng hoặc shop")
    @GetMapping("/applicable")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getApplicableVouchers(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false, defaultValue = "0") BigDecimal subtotal
    ) {
        List<VoucherResponse> response = voucherService.getApplicableVouchers(shopId, subtotal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Tra cứu thông tin chi tiết một Voucher theo mã code")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherByCode(@PathVariable String code) {
        VoucherResponse response = voucherService.getVoucherByCode(code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Lấy toàn bộ danh sách voucher trong hệ thống")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Voucher>>> getAllVouchers(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String status
    ) {
        List<Voucher> list = voucherRepository.findAll();
        if (shopId != null) {
            list = list.stream().filter(v -> shopId.equals(v.getShopId())).toList();
        }
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(v -> status.equalsIgnoreCase(v.getStatus())).toList();
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @Operation(summary = "Lấy thông tin voucher theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Voucher>> getVoucherById(@PathVariable Long id) {
        return voucherRepository.findById(id)
                .map(v -> ResponseEntity.ok(ApiResponse.success(v)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy voucher")));
    }

    @Operation(summary = "Tạo mới voucher")
    @PostMapping
    public ResponseEntity<ApiResponse<Voucher>> createVoucher(@RequestBody Voucher voucher) {
        if (voucher.getCode() == null || voucher.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã voucher không được để trống"));
        }
        if (voucherRepository.existsByCode(voucher.getCode().trim().toUpperCase())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã voucher đã tồn tại trên hệ thống"));
        }
        voucher.setCode(voucher.getCode().trim().toUpperCase());
        if (voucher.getStatus() == null) {
            voucher.setStatus("ACTIVE");
        }
        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo voucher thành công", saved));
    }

    @Operation(summary = "Cập nhật trạng thái voucher")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Voucher>> updateVoucherStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        Optional<Voucher> opt = voucherRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy voucher"));
        }
        Voucher voucher = opt.get();
        voucher.setStatus(status.toUpperCase());
        Voucher updated = voucherRepository.save(voucher);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái voucher thành công", updated));
    }

    @Operation(summary = "Xóa voucher")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long id) {
        if (!voucherRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy voucher"));
        }
        voucherRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa voucher thành công", null));
    }
}
