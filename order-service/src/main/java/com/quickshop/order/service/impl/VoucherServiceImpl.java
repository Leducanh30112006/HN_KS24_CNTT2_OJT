package com.quickshop.order.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.dto.voucher.ApplyVoucherResult;
import com.quickshop.order.dto.voucher.VoucherResponse;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.enums.VoucherDiscountType;
import com.quickshop.order.entity.enums.VoucherStatus;
import com.quickshop.order.repository.VoucherRepository;
import com.quickshop.order.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional
    public ApplyVoucherResult applyShopVoucher(String code, Long shopId, BigDecimal shopSubtotal) {
        if (code == null || code.trim().isEmpty() || shopId == null || shopSubtotal == null) {
            return ApplyVoucherResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .applied(false)
                    .build();
        }

        String trimmedCode = code.trim();
        Voucher voucher = voucherRepository.findByCodeAndStatus(trimmedCode, VoucherStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException(String.format("Voucher shop '%s' không tồn tại hoặc đã hết hiệu lực.", trimmedCode)));

        // 1, 2, 3. Kiểm tra điều kiện hợp lệ
        validateVoucherEligibility(voucher, shopId, shopSubtotal, false);

        // 4. Tính toán mức giảm chính xác
        BigDecimal discount = calculateDiscount(voucher, shopSubtotal);

        // 5. Cập nhật lượt dùng nguyên tử (Atomic Update) chống vượt usage_limit khi concurrent
        int rowsUpdated = voucherRepository.incrementUsageCountAtomic(voucher.getId(), VoucherStatus.ACTIVE);
        if (rowsUpdated == 0) {
            log.warn("Cạnh tranh checkout: Voucher shop '{}' đã hết lượt sử dụng trong tích tắc", trimmedCode);
            throw new BadRequestException(String.format("Voucher '%s' đã hết số lượt sử dụng.", trimmedCode));
        }

        log.info("Áp dụng thành công Voucher shop '{}' cho Shop ID: {}. Giảm: {} VND",
                trimmedCode, shopId, discount);

        return ApplyVoucherResult.builder()
                .code(trimmedCode)
                .voucherId(voucher.getId())
                .shopId(shopId)
                .discountAmount(discount)
                .applied(true)
                .build();
    }

    @Override
    @Transactional
    public ApplyVoucherResult applyPlatformVoucher(String code, BigDecimal totalMerchandiseAmount) {
        if (code == null || code.trim().isEmpty() || totalMerchandiseAmount == null) {
            return ApplyVoucherResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .applied(false)
                    .build();
        }

        String trimmedCode = code.trim();
        Voucher voucher = voucherRepository.findByCodeAndStatus(trimmedCode, VoucherStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException(String.format("Voucher toàn sàn '%s' không tồn tại hoặc đã hết hiệu lực.", trimmedCode)));

        // 1, 2, 3. Kiểm tra điều kiện hợp lệ
        validateVoucherEligibility(voucher, null, totalMerchandiseAmount, true);

        // 4. Tính toán mức giảm chính xác
        BigDecimal discount = calculateDiscount(voucher, totalMerchandiseAmount);

        // 5. Cập nhật lượt dùng nguyên tử (Atomic Update) chống vượt usage_limit khi concurrent
        int rowsUpdated = voucherRepository.incrementUsageCountAtomic(voucher.getId(), VoucherStatus.ACTIVE);
        if (rowsUpdated == 0) {
            log.warn("Cạnh tranh checkout: Voucher toàn sàn '{}' đã hết lượt sử dụng trong tích tắc", trimmedCode);
            throw new BadRequestException(String.format("Voucher toàn sàn '%s' đã hết số lượt sử dụng.", trimmedCode));
        }

        log.info("Áp dụng thành công Voucher toàn sàn '{}'. Giảm: {} VND", trimmedCode, discount);

        return ApplyVoucherResult.builder()
                .code(trimmedCode)
                .voucherId(voucher.getId())
                .shopId(null)
                .discountAmount(discount)
                .applied(true)
                .build();
    }

    @Override
    @Transactional
    public void releaseVoucherUsage(String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        String trimmedCode = code.trim();
        int rows = voucherRepository.decrementUsageCountByCodeAtomic(trimmedCode);
        if (rows > 0) {
            log.info("Hoàn tác lượt sử dụng cho voucher '{}' thành công (Compensating Transaction)", trimmedCode);
        } else {
            log.warn("Không thể hoàn tác lượt dùng cho voucher '{}' (có thể lượt dùng đã = 0 hoặc mã không tồn tại)", trimmedCode);
        }
    }

    @Override
    @Transactional
    public void releaseVouchers(String platformVoucherCode, Collection<String> shopVoucherCodes) {
        if (platformVoucherCode != null && !platformVoucherCode.trim().isEmpty()) {
            releaseVoucherUsage(platformVoucherCode);
        }
        if (shopVoucherCodes != null) {
            for (String shopCode : shopVoucherCodes) {
                if (shopCode != null && !shopCode.trim().isEmpty()) {
                    releaseVoucherUsage(shopCode);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Mã voucher không được để trống.");
        }
        String trimmedCode = code.trim();
        Voucher voucher = voucherRepository.findByCode(trimmedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với mã: " + trimmedCode));

        int remaining = Math.max(0, voucher.getUsageLimit() - voucher.getUsedCount());

        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .shopId(voucher.getShopId())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minOrderValue(voucher.getMinOrderValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .remainingUsage(remaining)
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .status(voucher.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplyVoucherResult previewVoucherDiscount(String code, Long shopId, BigDecimal orderAmount) {
        if (code == null || code.trim().isEmpty() || orderAmount == null) {
            return ApplyVoucherResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .applied(false)
                    .build();
        }

        String trimmedCode = code.trim();
        Voucher voucher = voucherRepository.findByCodeAndStatus(trimmedCode, VoucherStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException(String.format("Voucher '%s' không tồn tại hoặc đã hết hiệu lực.", trimmedCode)));

        boolean isPlatform = (shopId == null);
        validateVoucherEligibility(voucher, shopId, orderAmount, isPlatform);
        BigDecimal discount = calculateDiscount(voucher, orderAmount);

        return ApplyVoucherResult.builder()
                .code(trimmedCode)
                .voucherId(voucher.getId())
                .shopId(voucher.getShopId())
                .discountAmount(discount)
                .applied(true)
                .build();
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void validateVoucherEligibility(Voucher voucher, Long expectedShopId, BigDecimal orderAmount, boolean isPlatformVoucher) {
        // 1. Kiểm tra trạng thái
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new BadRequestException(String.format("Voucher '%s' hiện không ở trạng thái hoạt động (%s).",
                    voucher.getCode(), voucher.getStatus()));
        }

        // 1. Kiểm tra thời hạn
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getStartDate())) {
            throw new BadRequestException(String.format("Voucher '%s' chưa tới thời gian bắt đầu áp dụng (bắt đầu từ: %s).",
                    voucher.getCode(), voucher.getStartDate()));
        }
        if (now.isAfter(voucher.getEndDate())) {
            throw new BadRequestException(String.format("Voucher '%s' đã hết hạn sử dụng (hạn chót: %s).",
                    voucher.getCode(), voucher.getEndDate()));
        }

        // 3. Kiểm tra xem voucher có thuộc shop tương ứng không
        if (isPlatformVoucher) {
            if (voucher.getShopId() != null) {
                throw new BadRequestException(String.format("Voucher '%s' là voucher riêng của shop (Shop ID: %d), không thể dùng làm voucher toàn sàn.",
                        voucher.getCode(), voucher.getShopId()));
            }
        } else {
            if (voucher.getShopId() == null) {
                throw new BadRequestException(String.format("Voucher '%s' là voucher toàn sàn, không thể dùng làm voucher riêng của shop.",
                        voucher.getCode()));
            }
            if (!voucher.getShopId().equals(expectedShopId)) {
                throw new BadRequestException(String.format("Voucher '%s' không thuộc về Shop ID %d (chỉ áp dụng cho Shop ID %d).",
                        voucher.getCode(), expectedShopId, voucher.getShopId()));
            }
        }

        // 2. Kiểm tra giá trị đơn hàng tối thiểu
        if (voucher.getMinOrderValue() != null && orderAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException(String.format(
                    "Đơn hàng chưa đạt giá trị tối thiểu %s VND để áp dụng voucher '%s' (giá trị hiện tại: %s VND).",
                    voucher.getMinOrderValue(), voucher.getCode(), orderAmount
            ));
        }

        // 5. Kiểm tra sơ bộ lượt sử dụng
        if (voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BadRequestException(String.format("Voucher '%s' đã hết số lượt sử dụng (%d/%d).",
                    voucher.getCode(), voucher.getUsedCount(), voucher.getUsageLimit()));
        }
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal baseAmount) {
        BigDecimal discount = BigDecimal.ZERO;

        if (voucher.getDiscountType() == VoucherDiscountType.FIXED_AMOUNT) {
            discount = voucher.getDiscountValue();
        } else if (voucher.getDiscountType() == VoucherDiscountType.PERCENTAGE) {
            // Giảm theo %: (baseAmount * discountValue) / 100
            discount = baseAmount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

            // Kiểm tra mức giảm tối đa (max_discount_amount)
            if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        }

        // Mức giảm không bao giờ được vượt quá giá trị đơn hàng được áp dụng
        if (discount.compareTo(baseAmount) > 0) {
            discount = baseAmount;
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        return discount;
    }
}
