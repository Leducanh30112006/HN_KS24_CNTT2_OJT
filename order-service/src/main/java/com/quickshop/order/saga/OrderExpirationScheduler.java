package com.quickshop.order.saga;

import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.MasterOrderStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tác vụ định kỳ quét và xử lý timeout đơn hàng quá hạn 15 phút (TTL 15m):
 * - Tìm các đơn có trạng thái DRAFT hoặc PENDING_PAYMENT tạo cách đây hơn 15 phút
 * - Tự động kích hoạt bồi hoàn Saga: Hủy đơn, giải phóng tồn kho trên Catalog Service và hoàn voucher
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    public static final int ORDER_TIMEOUT_MINUTES = 15;

    private final MasterOrderRepository masterOrderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;

    @Scheduled(fixedRate = 60000) // Quét mỗi phút 1 lần
    public int checkAndCancelExpiredOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);
        log.debug("OrderExpirationScheduler: Kiểm tra các đơn hàng chưa thanh toán quá hạn trước {}", threshold);

        List<MasterOrder> expiredOrders = masterOrderRepository.findByStatusInAndCreatedAtBefore(
                List.of(MasterOrderStatus.DRAFT, MasterOrderStatus.PENDING_PAYMENT),
                threshold
        );

        if (expiredOrders.isEmpty()) {
            return 0;
        }

        log.info("OrderExpirationScheduler: Phát hiện {} đơn hàng quá hạn thanh toán 15 phút cần hủy và bồi hoàn",
                expiredOrders.size());

        int processed = 0;
        for (MasterOrder order : expiredOrders) {
            try {
                log.info("OrderExpirationScheduler: Đang hủy đơn hàng quá hạn #{} (Tạo lúc: {}, Trạng thái: {})",
                        order.getId(), order.getCreatedAt(), order.getStatus());

                sagaOrchestrator.cancelOrderWithCompensation(
                        order.getId(),
                        "Quá hạn thanh toán " + ORDER_TIMEOUT_MINUTES + " phút (Timeout Saga)"
                );
                processed++;
            } catch (Exception ex) {
                log.error("Lỗi khi bồi hoàn đơn hàng quá hạn #{}: {}", order.getId(), ex.getMessage(), ex);
            }
        }

        return processed;
    }
}
