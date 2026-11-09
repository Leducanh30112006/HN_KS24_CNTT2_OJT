package com.quickshop.order.saga;

import com.quickshop.common.event.*;
import com.quickshop.order.config.RabbitMQOrderConfig;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.MasterOrderStatus;
import com.quickshop.order.entity.OrderItem;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.SubOrderStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.OrderItemRepository;
import com.quickshop.order.repository.SubOrderRepository;
import com.quickshop.order.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Saga Orchestrator chịu trách nhiệm điều phối toàn bộ chu trình đặt hàng phân tán (Saga Pattern):
 * 1. Khởi tạo Saga khi người dùng Checkout (OrderCreatedEvent với TTL 15 phút).
 * 2. Nhận phản hồi tồn kho từ Catalog Service (StockReservedEvent).
 * 3. Kích hoạt bước thanh toán (PaymentRequestedEvent) hoặc xác nhận trực tiếp cho COD.
 * 4. Xử lý bồi hoàn (Compensating Transaction) khi thiếu hàng hoặc quá hạn 15 phút:
 *    - Giải phóng tồn kho (StockReleaseEvent).
 *    - Hoàn trả số lượt sử dụng Voucher sàn & shop.
 *    - Chuyển trạng thái đơn hàng sang CANCELLED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final RabbitTemplate rabbitTemplate;
    private final MasterOrderRepository masterOrderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final VoucherService voucherService;

    /**
     * Bắt đầu chu trình Saga Checkout sau khi đơn hàng được lưu vào DB
     */
    public void startCheckoutSaga(MasterOrder masterOrder, List<OrderItem> items) {
        log.info("Saga Orchestrator: Bắt đầu Checkout Saga cho MasterOrder #{}", masterOrder.getId());

        List<OrderItemEventDto> eventItems = toEventItems(items);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(String.valueOf(masterOrder.getId()))
                .masterOrderId(masterOrder.getId())
                .customerId(masterOrder.getUserId())
                .paymentMethod(masterOrder.getPaymentMethod())
                .totalAmount(masterOrder.getTotalAmount())
                .items(eventItems)
                .ttlSeconds(900) // 15 phút
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQOrderConfig.EXCHANGE_ORDER,
                RabbitMQOrderConfig.ROUTING_KEY_ORDER_CREATED,
                event
        );

        log.info("Saga Orchestrator: Đã phát OrderCreatedEvent (TTL 900s) cho MasterOrder #{}", masterOrder.getId());
    }

    /**
     * Xử lý khi Catalog Service giữ kho thành công
     */
    @Transactional
    public void handleStockReserved(StockReservedEvent event) {
        Long masterOrderId = extractMasterOrderId(event.getMasterOrderId(), event.getOrderId());
        log.info("Saga Orchestrator: Nhận StockReservedEvent thành công cho MasterOrder #{}", masterOrderId);

        MasterOrder masterOrder = masterOrderRepository.findById(masterOrderId).orElse(null);
        if (masterOrder == null) {
            log.warn("Không tìm thấy MasterOrder #{} khi xử lý StockReservedEvent", masterOrderId);
            return;
        }

        if (masterOrder.getStatus() == MasterOrderStatus.CANCELLED) {
            log.warn("MasterOrder #{} đã bị hủy trước đó, kích hoạt giải phóng kho bồi hoàn ngay lập tức", masterOrderId);
            publishStockRelease(masterOrder, "Đơn hàng đã hủy trước khi nhận kết quả giữ kho");
            return;
        }

        if ("COD".equalsIgnoreCase(masterOrder.getPaymentMethod())) {
            // Đơn COD: Sau khi giữ kho thành công -> chuyển sang PROCESSING và xác nhận SubOrders
            masterOrder.setStatus(MasterOrderStatus.PROCESSING);
            masterOrderRepository.save(masterOrder);

            updateSubOrdersStatus(masterOrderId, SubOrderStatus.CONFIRMED);

            // Gửi OrderConfirmedEvent để Catalog commit kho vĩnh viễn và Notification gửi email
            publishOrderConfirmed(masterOrder);
            log.info("Saga Orchestrator: Đơn COD #{} đã chuyển sang PROCESSING và xác nhận SubOrders", masterOrderId);
        } else {
            // Đơn Online (VNPay): Chuyển sang PENDING_PAYMENT và yêu cầu thanh toán với timeout 15 phút
            masterOrder.setStatus(MasterOrderStatus.PENDING_PAYMENT);
            masterOrderRepository.save(masterOrder);

            PaymentRequestedEvent paymentRequest = PaymentRequestedEvent.builder()
                    .orderId(String.valueOf(masterOrderId))
                    .masterOrderId(masterOrderId)
                    .userId(masterOrder.getUserId())
                    .amount(masterOrder.getTotalAmount())
                    .paymentMethod(masterOrder.getPaymentMethod())
                    .timeoutSeconds(900)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQOrderConfig.EXCHANGE_ORDER,
                    RabbitMQOrderConfig.ROUTING_KEY_PAYMENT_REQUESTED,
                    paymentRequest
            );

            log.info("Saga Orchestrator: Đã phát PaymentRequestedEvent cho MasterOrder #{} với hạn thanh toán 15 phút", masterOrderId);
        }
    }

    /**
     * Xử lý khi Catalog Service báo giữ kho thất bại (hết hàng)
     */
    @Transactional
    public void handleStockFailed(StockReservedEvent event) {
        Long masterOrderId = extractMasterOrderId(event.getMasterOrderId(), event.getOrderId());
        String reason = event.getReason() != null ? event.getReason() : "Hết hàng tồn kho trong hệ thống";
        log.warn("Saga Orchestrator: Giữ kho thất bại cho MasterOrder #{}, lý do: {}", masterOrderId, reason);

        cancelOrderWithCompensation(masterOrderId, "Giữ kho thất bại: " + reason);
    }

    /**
     * Bồi hoàn toàn bộ chu trình Saga:
     * - Cập nhật trạng thái MasterOrder & SubOrders sang CANCELLED
     * - Hoàn trả Voucher sàn và Voucher shop
     * - Phát sự kiện giải phóng kho StockReleaseEvent tới Catalog Service
     */
    @Transactional
    public void cancelOrderWithCompensation(Long masterOrderId, String reason) {
        log.info("Saga Orchestrator: Thực thi giao dịch bồi hoàn (Compensating Transaction) cho MasterOrder #{}, lý do: {}",
                masterOrderId, reason);

        MasterOrder masterOrder = masterOrderRepository.findById(masterOrderId).orElse(null);
        if (masterOrder == null) {
            log.warn("Không tìm thấy MasterOrder #{} để bồi hoàn", masterOrderId);
            return;
        }

        if (masterOrder.getStatus() == MasterOrderStatus.CANCELLED) {
            log.info("MasterOrder #{} đã ở trạng thái CANCELLED, bỏ qua", masterOrderId);
            return;
        }

        // 1. Cập nhật trạng thái MasterOrder -> CANCELLED
        masterOrder.setStatus(MasterOrderStatus.CANCELLED);
        masterOrderRepository.save(masterOrder);

        // 2. Cập nhật SubOrders -> CANCELLED
        updateSubOrdersStatus(masterOrderId, SubOrderStatus.CANCELLED);

        // 3. Hoàn trả số lượt dùng Voucher
        try {
            voucherService.revertUsage(masterOrderId);
            log.info("Đã hoàn trả lượt sử dụng voucher cho đơn #{}", masterOrderId);
        } catch (Exception ex) {
            log.warn("Lỗi khi hoàn trả voucher cho đơn #{}: {}", masterOrderId, ex.getMessage());
        }

        // 4. Phát lệnh giải phóng kho StockReleaseEvent
        publishStockRelease(masterOrder, reason);

        log.info("Saga Orchestrator: Hoàn tất giao dịch bồi hoàn cho MasterOrder #{}", masterOrderId);
    }

    /**
     * Xử lý khi Payment Service báo thanh toán thành công
     */
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Long masterOrderId = extractMasterOrderId(event.getMasterOrderId(), event.getOrderId());
        log.info("Saga Orchestrator: Nhận PaymentCompletedEvent cho MasterOrder #{}", masterOrderId);

        MasterOrder masterOrder = masterOrderRepository.findById(masterOrderId).orElse(null);
        if (masterOrder == null) {
            log.warn("Không tìm thấy MasterOrder #{} khi nhận thanh toán thành công", masterOrderId);
            return;
        }

        if (masterOrder.getStatus() == MasterOrderStatus.CANCELLED) {
            log.warn("MasterOrder #{} đã bị hủy/quá hạn nhưng nhận được thanh toán! Cần xử lý hoàn tiền (Refund)", masterOrderId);
            return;
        }

        masterOrder.setStatus(MasterOrderStatus.PROCESSING);
        masterOrderRepository.save(masterOrder);

        updateSubOrdersStatus(masterOrderId, SubOrderStatus.CONFIRMED);

        publishOrderConfirmed(masterOrder);
        log.info("Saga Orchestrator: Đã xác nhận MasterOrder #{} và gửi thông điệp OrderConfirmedEvent", masterOrderId);
    }

    /**
     * Xử lý khi Payment Service báo thanh toán thất bại
     */
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Long masterOrderId = extractMasterOrderId(event.getMasterOrderId(), event.getOrderId());
        log.warn("Saga Orchestrator: Nhận PaymentFailedEvent cho MasterOrder #{}, lý do: {}",
                masterOrderId, event.getReason());

        cancelOrderWithCompensation(masterOrderId, "Thanh toán thất bại: " + event.getReason());
    }

    private void updateSubOrdersStatus(Long masterOrderId, SubOrderStatus status) {
        List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(masterOrderId);
        for (SubOrder subOrder : subOrders) {
            subOrder.setStatus(status);
            subOrderRepository.save(subOrder);
        }
    }

    private void publishStockRelease(MasterOrder masterOrder, String reason) {
        List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        List<Long> subOrderIds = subOrders.stream().map(SubOrder::getId).toList();
        List<OrderItem> items = orderItemRepository.findBySubOrderIdIn(subOrderIds);

        List<OrderItemEventDto> eventItems = toEventItems(items);

        StockReleaseEvent releaseEvent = StockReleaseEvent.builder()
                .orderId(String.valueOf(masterOrder.getId()))
                .masterOrderId(masterOrder.getId())
                .items(eventItems)
                .reason(reason)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQOrderConfig.EXCHANGE_ORDER,
                RabbitMQOrderConfig.ROUTING_KEY_STOCK_RELEASE,
                releaseEvent
        );

        // Cũng phát thông báo compensation
        OrderCompensationEvent compensationEvent = OrderCompensationEvent.builder()
                .orderId(String.valueOf(masterOrder.getId()))
                .masterOrderId(masterOrder.getId())
                .triggerService("ORDER_SERVICE")
                .reason(reason)
                .items(eventItems)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQOrderConfig.EXCHANGE_ORDER,
                RabbitMQOrderConfig.ROUTING_KEY_ORDER_COMPENSATION,
                compensationEvent
        );

        log.info("Saga Orchestrator: Đã phát StockReleaseEvent & OrderCompensationEvent cho MasterOrder #{}", masterOrder.getId());
    }

    private void publishOrderConfirmed(MasterOrder masterOrder) {
        List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        List<Long> subOrderIds = subOrders.stream().map(SubOrder::getId).toList();
        List<OrderItem> items = orderItemRepository.findBySubOrderIdIn(subOrderIds);

        List<OrderItemEventDto> eventItems = toEventItems(items);

        OrderConfirmedEvent confirmedEvent = OrderConfirmedEvent.builder()
                .orderId(String.valueOf(masterOrder.getId()))
                .masterOrderId(masterOrder.getId())
                .userId(masterOrder.getUserId())
                .subOrderIds(subOrderIds)
                .paymentMethod(masterOrder.getPaymentMethod())
                .totalAmount(masterOrder.getTotalAmount())
                .items(eventItems)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQOrderConfig.EXCHANGE_ORDER,
                RabbitMQOrderConfig.ROUTING_KEY_ORDER_CONFIRMED,
                confirmedEvent
        );

        log.info("Saga Orchestrator: Đã phát OrderConfirmedEvent cho MasterOrder #{}", masterOrder.getId());
    }

    private List<OrderItemEventDto> toEventItems(List<OrderItem> items) {
        List<OrderItemEventDto> eventItems = new ArrayList<>();
        if (items != null) {
            for (OrderItem item : items) {
                eventItems.add(OrderItemEventDto.builder()
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .productName(item.getProductName())
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build());
            }
        }
        return eventItems;
    }

    private Long extractMasterOrderId(Long masterOrderId, String orderId) {
        if (masterOrderId != null) {
            return masterOrderId;
        }
        if (orderId != null && !orderId.isBlank()) {
            try {
                return Long.parseLong(orderId.trim());
            } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }
}
