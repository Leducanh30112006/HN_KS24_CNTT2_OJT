package com.quickshop.order.scheduler;

import com.quickshop.order.saga.OrderSagaCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderSagaCoordinator orderSagaCoordinator;

    /**
     * Tác vụ 1 (SRS QuickShop 2.2.0 - FR-07.3):
     * Quét định kỳ mỗi 5 phút các đơn PENDING_PAYMENT / DRAFT quá hạn 15 phút để nhả kho giữ và hoàn voucher.
     * Sử dụng ShedLock với lockAtMostFor 4 phút và lockAtLeastFor 30 giây nhằm chống chạy trùng giữa các container instance.
     */
    @Scheduled(cron = "${order.timeout.cron:0 */5 * * * *}")
    @SchedulerLock(
            name = "OrderTimeoutScheduler_scanAndCancelExpiredOrders",
            lockAtMostFor = "PT4M",
            lockAtLeastFor = "PT30S"
    )
    public void scanAndCancelExpiredOrders() {
        log.info("[Scheduler] Bắt đầu quét đơn hàng PENDING_PAYMENT quá hạn 15 phút...");
        try {
            int cancelled = orderSagaCoordinator.cancelExpiredOrders();
            if (cancelled > 0) {
                log.info("[Scheduler] Đã hủy và gửi bù trừ kho thành công cho {} đơn hàng quá hạn 15 phút", cancelled);
            } else {
                log.info("[Scheduler] Không có đơn hàng quá hạn nào cần hủy.");
            }
        } catch (Exception ex) {
            log.error("[Scheduler] Lỗi trong quá trình quét đơn hàng timeout: {}", ex.getMessage(), ex);
        }
    }
}
