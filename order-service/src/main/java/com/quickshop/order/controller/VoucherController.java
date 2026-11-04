package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherRepository voucherRepository;

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Voucher>> getVoucherById(@PathVariable Long id) {
        return voucherRepository.findById(id)
                .map(v -> ResponseEntity.ok(ApiResponse.success(v)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy voucher")));
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long id) {
        if (!voucherRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy voucher"));
        }
        voucherRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa voucher thành công", null));
    }
}
