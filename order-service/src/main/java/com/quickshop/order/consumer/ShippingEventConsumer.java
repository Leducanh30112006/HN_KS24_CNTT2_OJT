package com.quickshop.order.consumer;

import com.quickshop.common.event.ShipmentStatusUpdatedEvent;
import com.quickshop.order.config.RabbitMQOrderConfig;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.SubOrderStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.SubOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consumer lắng nghe sự kiện cập nhật hành trình vận đơn từ Shipping & Logistics Service.
 * Thực thi State Machine cho Sub-Order và Master Order theo SRS 2.2.0:
 * READY_TO_PICK -> PACKING
 * PICKED / DELIVERING -> SHIPPING
 * DELIVERED -> DELIVERED (Kích hoạt đếm ngược 3 ngày Escrow)
 * DELIVERY_FAILED -> DELIVERY_FAILED
 * RETURNED -> CANCELLED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingEventConsumer {

    private final SubOrderRepository subOrderRepository;
    private final MasterOrderRepository masterOrderRepository;

    @RabbitListener(queues = RabbitMQOrderConfig.QUEUE_ORDER_SHIPPING)
    @Transactional
    public void onShipmentStatusUpdated(ShipmentStatusUpdatedEvent event) {
        log.info("[Shipping Consumer] Nhận ShipmentStatusUpdatedEvent: subOrderId={}, status={}, trackingCode={}",
                event.getSubOrderId(), event.getShippingStatus(), event.getTrackingCode());

        if (event.getSubOrderId() == null) {
            log.warn("[Shipping Consumer] subOrderId null, bỏ qua xử lý");
            return;
        }

        SubOrder subOrder = subOrderRepository.findById(event.getSubOrderId()).orElse(null);
        if (subOrder == null) {
            log.warn("[Shipping Consumer] Không tìm thấy Sub-Order #{}", event.getSubOrderId());
            return;
        }

        SubOrderStatus targetStatus = mapShippingStatusToSubOrderStatus(event.getShippingStatus());
        if (targetStatus != null && subOrder.getStatus() != targetStatus) {
            log.info("[Shipping Consumer] Cập nhật Sub-Order #{} từ {} -> {}",
                    subOrder.getId(), subOrder.getStatus(), targetStatus);
            subOrder.setStatus(targetStatus);
            subOrderRepository.save(subOrder);

            // Kiểm tra trạng thái Master Order
            if (subOrder.getMasterOrder() != null) {
                updateMasterOrderStatusIfEligible(subOrder.getMasterOrder().getId());
            }
        }
    }

    private SubOrderStatus mapShippingStatusToSubOrderStatus(String shippingStatus) {
        if (shippingStatus == null) return null;
        return switch (shippingStatus.toUpperCase()) {
            case "READY_TO_PICK" -> SubOrderStatus.PACKING;
            case "PICKED", "DELIVERING" -> SubOrderStatus.SHIPPING;
            case "DELIVERED" -> SubOrderStatus.DELIVERED;
            case "DELIVERY_FAILED" -> SubOrderStatus.DELIVERY_FAILED;
            case "RETURNED" -> SubOrderStatus.CANCELLED;
            default -> null;
        };
    }

    private void updateMasterOrderStatusIfEligible(Long masterOrderId) {
        if (masterOrderId == null) return;

        MasterOrder masterOrder = masterOrderRepository.findById(masterOrderId).orElse(null);
        if (masterOrder == null) return;

        List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(masterOrderId);
        if (subOrders.isEmpty()) return;

        boolean allDeliveredOrCompleted = subOrders.stream().allMatch(so ->
                so.getStatus() == SubOrderStatus.DELIVERED || so.getStatus() == SubOrderStatus.COMPLETED);

        if (allDeliveredOrCompleted && masterOrder.getStatus() != MasterOrderStatus.COMPLETED) {
            log.info("[Shipping Consumer] Toàn bộ Sub-Orders của Master Order #{} đã giao xong. Cập nhật Master Order sang COMPLETED",
                    masterOrderId);
            masterOrder.setStatus(MasterOrderStatus.COMPLETED);
            masterOrderRepository.save(masterOrder);
        }
    }
}
