package com.quickshop.order.config;

import com.quickshop.order.entity.Voucher;
import com.quickshop.order.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDataInitializer {

    private final VoucherRepository voucherRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedInitialVouchers() {
        if (voucherRepository.count() == 0) {
            log.info("Bắt đầu khởi tạo dữ liệu mẫu Voucher cho hệ thống QuickShop...");

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate = now.plusYears(2);

            List<Voucher> initialVouchers = List.of(
                    Voucher.builder()
                            .code("QUICKSHOP50K")
                            .shopId(null) // Voucher toàn sàn
                            .discountType("FIXED_AMOUNT")
                            .discountValue(BigDecimal.valueOf(50000))
                            .minOrderValue(BigDecimal.valueOf(200000))
                            .maxDiscountAmount(BigDecimal.valueOf(50000))
                            .usageLimit(10000)
                            .usedCount(0)
                            .startDate(now.minusDays(1))
                            .endDate(endDate)
                            .status("ACTIVE")
                            .build(),
                    Voucher.builder()
                            .code("FREESHIPMAX")
                            .shopId(null) // Voucher freeship toàn sàn
                            .discountType("FIXED_AMOUNT")
                            .discountValue(BigDecimal.valueOf(25000))
                            .minOrderValue(BigDecimal.valueOf(99000))
                            .maxDiscountAmount(BigDecimal.valueOf(25000))
                            .usageLimit(10000)
                            .usedCount(0)
                            .startDate(now.minusDays(1))
                            .endDate(endDate)
                            .status("ACTIVE")
                            .build(),
                    Voucher.builder()
                            .code("MALL100K")
                            .shopId(null)
                            .discountType("FIXED_AMOUNT")
                            .discountValue(BigDecimal.valueOf(100000))
                            .minOrderValue(BigDecimal.valueOf(1000000))
                            .maxDiscountAmount(BigDecimal.valueOf(100000))
                            .usageLimit(10000)
                            .usedCount(0)
                            .startDate(now.minusDays(1))
                            .endDate(endDate)
                            .status("ACTIVE")
                            .build(),
                    Voucher.builder()
                            .code("SUPERDEAL15")
                            .shopId(null)
                            .discountType("PERCENTAGE")
                            .discountValue(BigDecimal.valueOf(15))
                            .minOrderValue(BigDecimal.valueOf(500000))
                            .maxDiscountAmount(BigDecimal.valueOf(150000))
                            .usageLimit(10000)
                            .usedCount(0)
                            .startDate(now.minusDays(1))
                            .endDate(endDate)
                            .status("ACTIVE")
                            .build(),
                    Voucher.builder()
                            .code("FREESHIP50K")
                            .shopId(null)
                            .discountType("FIXED_AMOUNT")
                            .discountValue(BigDecimal.valueOf(50000))
                            .minOrderValue(BigDecimal.valueOf(300000))
                            .maxDiscountAmount(BigDecimal.valueOf(50000))
                            .usageLimit(10000)
                            .usedCount(0)
                            .startDate(now.minusDays(1))
                            .endDate(endDate)
                            .status("ACTIVE")
                            .build()
            );

            voucherRepository.saveAll(initialVouchers);
            log.info("Khởi tạo thành công {} Voucher mẫu toàn sàn vào cơ sở dữ liệu order_db.", initialVouchers.size());
        }
    }
}

