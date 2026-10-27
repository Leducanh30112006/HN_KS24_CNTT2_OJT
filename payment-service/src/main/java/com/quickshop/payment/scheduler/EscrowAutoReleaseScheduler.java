package com.quickshop.payment.scheduler;

import com.quickshop.payment.service.EscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "escrow.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class EscrowAutoReleaseScheduler {

    private final EscrowService escrowService;

    /**
     * Tác vụ 2: Tự động Giải phóng Escrow (SRS QuickShop FR-07.3)
     * Chạy lúc 00:00 hàng ngày, chuyển tiền vào ví Seller cho các Sub-Order DELIVERED quá 3 ngày không khiếu nại.
     * Sử dụng ShedLock với lockAtMostFor 30 phút và lockAtLeastFor 5 phút để chống chạy trùng giữa các container instance.
     */
    @Scheduled(cron = "${escrow.auto-release.cron:0 0 0 * * ?}")
    @SchedulerLock(
            name = "EscrowAutoReleaseScheduler_scheduleAutoRelease",
            lockAtMostFor = "PT30M",
            lockAtLeastFor = "PT5M"
    )
    public void scheduleAutoRelease() {
        log.info("[Scheduler] Bắt đầu tác vụ định kỳ 00:00 tự động giải phóng Escrow cho đơn hàng DELIVERED quá 3 ngày...");
        try {
            int count = escrowService.autoReleaseEligibleEscrows();
            log.info("[Scheduler] Hoàn tất giải phóng tự động cho {} tài khoản Escrow.", count);
        } catch (Exception e) {
            log.error("[Scheduler] Lỗi trong quá trình tự động giải phóng Escrow: {}", e.getMessage(), e);
        }
    }
}
