package com.quickshop.order.service.impl;

import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.dto.voucher.*;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.VoucherUsage;
import com.quickshop.order.repository.VoucherRepository;
import com.quickshop.order.repository.VoucherUsageRepository;
import com.quickshop.order.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Override
    @Transactional(readOnly = true)
    public VoucherValidationResponse validateAndCalculateVoucher(String code, Long shopId, BigDecimal orderSubtotal) {
        if (code == null || code.trim().isBlank()) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .message("Mã voucher không được để trống")
                    .discountAmount(BigDecimal.ZERO)
                    .build();
        }

        String cleanCode = code.trim().toUpperCase();
        Optional<Voucher> optVoucher = voucherRepository.findByCode(cleanCode);

        if (optVoucher.isEmpty()) {
            return VoucherValidationResponse.builder()
                    .code(cleanCode)
                    .valid(false)
                    .message(String.format("Mã voucher '%s' không tồn tại trên hệ thống", cleanCode))
                    .discountAmount(BigDecimal.ZERO)
                    .build();
        }

        Voucher voucher = optVoucher.get();
        boolean isPlatformVoucher = (voucher.getShopId() == null);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal subtotal = (orderSubtotal != null && orderSubtotal.compareTo(BigDecimal.ZERO) > 0)
                ? orderSubtotal
                : BigDecimal.ZERO;

        // 1. Kiểm tra trạng thái hoạt động
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            return buildInvalidResponse(voucher, "Voucher hiện không ở trạng thái hoạt động");
        }

        // 2. Kiểm tra thời hạn hiệu lực (start_date & end_date)
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            return buildInvalidResponse(voucher, String.format("Voucher chưa tới thời gian áp dụng (bắt đầu từ %s)", voucher.getStartDate()));
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            return buildInvalidResponse(voucher, String.format("Voucher đã hết hạn sử dụng (hết hạn lúc %s)", voucher.getEndDate()));
        }

        // 3. Kiểm tra số lượt sử dụng
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return buildInvalidResponse(voucher, "Voucher đã hết lượt sử dụng");
        }

        // 4. Kiểm tra phạm vi áp dụng (Voucher Sàn vs Voucher của Shop)
        if (shopId != null) {
            // Đang áp dụng cho 1 Shop cụ thể:
            // Phải là Voucher của chính Shop đó
            if (isPlatformVoucher) {
                return buildInvalidResponse(voucher, "Đây là Voucher toàn sàn, vui lòng nhập vào ô Voucher Sàn");
            }
            if (!voucher.getShopId().equals(shopId)) {
                return buildInvalidResponse(voucher, String.format("Voucher này chỉ áp dụng cho Shop #%d, không áp dụng cho Shop #%d", voucher.getShopId(), shopId));
            }
        } else {
            // Đang áp dụng cho toàn sàn:
            // Phải là Voucher toàn sàn (shopId == null)
            if (!isPlatformVoucher) {
                return buildInvalidResponse(voucher, String.format("Đây là Voucher của Shop #%d, không thể áp dụng cho toàn sàn", voucher.getShopId()));
            }
        }

        // 5. Kiểm tra giá trị đơn hàng tối thiểu (min_order_value)
        if (voucher.getMinOrderValue() != null && subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            return buildInvalidResponse(voucher, String.format("Đơn hàng chưa đạt giá trị tối thiểu %s VND để sử dụng voucher này", voucher.getMinOrderValue()));
        }

        // 6. Tính toán mức giảm giá chi tiết
        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENTAGE".equalsIgnoreCase(voucher.getDiscountType())) {
            if (voucher.getDiscountValue() != null) {
                discount = subtotal.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

                if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                    discount = voucher.getMaxDiscountAmount();
                }
            }
        } else {
            // FIXED_AMOUNT
            if (voucher.getDiscountValue() != null) {
                discount = voucher.getDiscountValue();
                if (discount.compareTo(subtotal) > 0) {
                    discount = subtotal; // Không giảm vượt quá giá trị tiền hàng
                }
            }
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        log.info("Xác thực thành công Voucher [{}] cho Shop [{}]: giảm {} VND", cleanCode, shopId, discount);

        return VoucherValidationResponse.builder()
                .code(voucher.getCode())
                .shopId(voucher.getShopId())
                .platformVoucher(isPlatformVoucher)
                .valid(true)
                .message("Áp dụng voucher thành công")
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .discountAmount(discount)
                .minOrderValue(voucher.getMinOrderValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherApplyResponse calculateDiscounts(VoucherApplyRequest request) {
        if (request == null) {
            return VoucherApplyResponse.builder().build();
        }

        Map<Long, VoucherValidationResponse> shopVoucherResults = new LinkedHashMap<>();
        BigDecimal totalShopDiscount = BigDecimal.ZERO;
        BigDecimal totalOrderSubtotal = BigDecimal.ZERO;

        // 1. Tính toán Voucher từng Shop
        if (request.getShopSubtotals() != null) {
            for (Map.Entry<Long, BigDecimal> entry : request.getShopSubtotals().entrySet()) {
                Long shopId = entry.getKey();
                BigDecimal shopSubtotal = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
                totalOrderSubtotal = totalOrderSubtotal.add(shopSubtotal);

                String shopVoucherCode = (request.getShopVouchers() != null) ? request.getShopVouchers().get(shopId) : null;
                if (shopVoucherCode != null && !shopVoucherCode.isBlank()) {
                    VoucherValidationResponse shopResult = validateAndCalculateVoucher(shopVoucherCode, shopId, shopSubtotal);
                    shopVoucherResults.put(shopId, shopResult);
                    if (shopResult.isValid() && shopResult.getDiscountAmount() != null) {
                        totalShopDiscount = totalShopDiscount.add(shopResult.getDiscountAmount());
                    }
                }
            }
        }

        // 2. Tính toán Voucher toàn sàn
        VoucherValidationResponse platformResult = null;
        BigDecimal totalPlatformDiscount = BigDecimal.ZERO;

        if (request.getPlatformVoucherCode() != null && !request.getPlatformVoucherCode().isBlank()) {
            platformResult = validateAndCalculateVoucher(request.getPlatformVoucherCode(), null, totalOrderSubtotal);
            if (platformResult.isValid() && platformResult.getDiscountAmount() != null) {
                totalPlatformDiscount = platformResult.getDiscountAmount();
            }
        }

        BigDecimal totalDiscount = totalShopDiscount.add(totalPlatformDiscount);

        return VoucherApplyResponse.builder()
                .platformVoucher(platformResult)
                .shopVouchers(shopVoucherResults)
                .totalPlatformDiscount(totalPlatformDiscount)
                .totalShopDiscount(totalShopDiscount)
                .totalDiscount(totalDiscount)
                .build();
    }

    @Override
    @Transactional
    public void recordUsage(String voucherCode, Long userId, Long masterOrderId, Long subOrderId, BigDecimal discountAmount) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return;
        }

        String cleanCode = voucherCode.trim().toUpperCase();
        voucherRepository.findByCode(cleanCode).ifPresent(voucher -> {
            // 1. Lưu bản ghi sử dụng
            VoucherUsage usage = VoucherUsage.builder()
                    .voucherId(voucher.getId())
                    .voucherCode(voucher.getCode())
                    .userId(userId)
                    .masterOrderId(masterOrderId)
                    .subOrderId(subOrderId)
                    .discountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                    .build();

            voucherUsageRepository.save(usage);

            // 2. Tăng số lượt đã dùng
            int currentUsed = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
            voucher.setUsedCount(currentUsed + 1);

            // 3. Nếu đã đạt giới hạn lượt dùng, cập nhật trạng thái
            if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
                voucher.setStatus("EXHAUSTED");
                log.info("Voucher [{}] đã đạt giới hạn sử dụng {} lượt -> chuyển trạng thái EXHAUSTED",
                        voucher.getCode(), voucher.getUsageLimit());
            }

            voucherRepository.save(voucher);
            log.info("Đã ghi nhận sử dụng Voucher [{}] cho user #{} đơn hàng Master #{}, số tiền giảm: {}",
                    cleanCode, userId, masterOrderId, discountAmount);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getApplicableVouchers(Long shopId, BigDecimal subtotal) {
        List<Voucher> list;
        if (shopId != null) {
            list = voucherRepository.findByShopIdAndStatus(shopId, "ACTIVE");
        } else {
            list = voucherRepository.findByShopIdIsNullAndStatus("ACTIVE");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal minAmount = subtotal != null ? subtotal : BigDecimal.ZERO;

        return list.stream()
                .filter(v -> v.getStartDate() == null || !now.isBefore(v.getStartDate()))
                .filter(v -> v.getEndDate() == null || !now.isAfter(v.getEndDate()))
                .filter(v -> v.getUsageLimit() == null || v.getUsedCount() < v.getUsageLimit())
                .filter(v -> v.getMinOrderValue() == null || minAmount.compareTo(v.getMinOrderValue()) >= 0)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResourceNotFoundException("Mã voucher không được để trống");
        }

        return voucherRepository.findByCode(code.trim().toUpperCase())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với mã " + code));
    }

    @Override
    @Transactional
    public void revertUsage(Long masterOrderId) {
        if (masterOrderId == null) {
            return;
        }

        List<VoucherUsage> usages = voucherUsageRepository.findByMasterOrderId(masterOrderId);
        if (usages.isEmpty()) {
            log.info("Không có voucher nào được áp dụng cho đơn hàng Master #{} cần hoàn trả", masterOrderId);
            return;
        }

        for (VoucherUsage usage : usages) {
            voucherRepository.findById(usage.getVoucherId()).ifPresent(voucher -> {
                int currentUsed = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
                voucher.setUsedCount(Math.max(0, currentUsed - 1));

                // Nếu voucher từng bị EXHAUSTED nhưng giờ số lượt dùng giảm xuống dưới giới hạn -> phục hồi ACTIVE
                if ("EXHAUSTED".equalsIgnoreCase(voucher.getStatus())
                        && voucher.getUsageLimit() != null
                        && voucher.getUsedCount() < voucher.getUsageLimit()) {
                    voucher.setStatus("ACTIVE");
                    log.info("Voucher [{}] được phục hồi trạng thái ACTIVE sau khi bồi hoàn đơn #{}",
                            voucher.getCode(), masterOrderId);
                }

                voucherRepository.save(voucher);
                log.info("Đã hoàn trả 1 lượt dùng cho Voucher [{}] (usedCount: {}) từ đơn Master #{}",
                        voucher.getCode(), voucher.getUsedCount(), masterOrderId);
            });
        }

        voucherUsageRepository.deleteAll(usages);
        log.info("Đã xóa {} bản ghi sử dụng voucher của đơn hàng Master #{}", usages.size(), masterOrderId);
    }

    private VoucherValidationResponse buildInvalidResponse(Voucher voucher, String message) {
        return VoucherValidationResponse.builder()
                .code(voucher.getCode())
                .shopId(voucher.getShopId())
                .platformVoucher(voucher.getShopId() == null)
                .valid(false)
                .message(message)
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .discountAmount(BigDecimal.ZERO)
                .minOrderValue(voucher.getMinOrderValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .build();
    }

    private VoucherResponse mapToResponse(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .shopId(v.getShopId())
                .platformVoucher(v.getShopId() == null)
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .minOrderValue(v.getMinOrderValue())
                .maxDiscountAmount(v.getMaxDiscountAmount())
                .usageLimit(v.getUsageLimit())
                .usedCount(v.getUsedCount())
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .status(v.getStatus())
                .build();
    }
}
