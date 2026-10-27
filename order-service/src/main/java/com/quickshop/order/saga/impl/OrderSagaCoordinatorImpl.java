package com.quickshop.order.saga.impl;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.common.event.OrderItemEventDto;
import com.quickshop.common.event.RollbackEvent;
import com.quickshop.common.event.StockReservedEvent;
import com.quickshop.order.config.RabbitMQOrderConfig;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.OrderItem;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.SubOrderStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.saga.OrderSagaCoordinator;
import com.quickshop.order.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaCoordinatorImpl implements OrderSagaCoordinator {

    private final RabbitTemplate rabbitTemplate;
    private final MasterOrderRepository masterOrderRepository;
    private final VoucherService voucherService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SAGA_KEY_PREFIX = "saga:order:";
    private static final String SAGA_STOCK_RESERVED_KEY = "saga:stock:reserved:processed:";
    private static final String SAGA_STOCK_FAILED_KEY = "saga:stock:failed:processed:";
    private static final Duration SAGA_TTL = Duration.ofHours(24);

    @Override
    @Transactional
    public void initiateCheckoutSaga(MasterOrder masterOrder) {
        if (masterOrder == null || masterOrder.getId() == null) {
            log.error("[Saga Orchestrator] Không thể khởi chạy Saga: MasterOrder null hoặc chưa được lưu");
            return;
        }

        Long orderId = masterOrder.getId();
        String correlationId = UUID.randomUUID().toString();
        String messageId = UUID.randomUUID().toString();

        log.info("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Bắt đầu Saga Workflow: Chuẩn bị gửi lệnh Reserve Stock sang Catalog Service",
                orderId, correlationId);

        // 1. Chuyển đổi danh sách snapshot items sang OrderItemEventDto cho Catalog Service
        List<OrderItemEventDto> eventItems = new ArrayList<>();
        if (masterOrder.getSubOrders() != null) {
            for (SubOrder subOrder : masterOrder.getSubOrders()) {
                if (subOrder.getItems() != null) {
                    for (OrderItem item : subOrder.getItems()) {
                        eventItems.add(OrderItemEventDto.builder()
                                .productId(item.getVariantId()) // Catalog sử dụng productId để tra cứu SKU/Variant
                                .productName(item.getProductName())
                                .sku(item.getVariantName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build());
                    }
                }
            }
        }

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(String.valueOf(orderId))
                .customerId(masterOrder.getUserId())
                .customerEmail(masterOrder.getRecipientPhone())
                .items(eventItems)
                .totalAmount(masterOrder.getTotalAmount())
                .timestamp(LocalDateTime.now())
                .build();

        // 2. Ghi nhận Saga State vào Redis để giám sát tiến độ
        stringRedisTemplate.opsForValue().set(SAGA_KEY_PREFIX + orderId + ":status", "STOCK_RESERVE_PENDING", SAGA_TTL);
        stringRedisTemplate.opsForValue().set(SAGA_KEY_PREFIX + orderId + ":correlation_id", correlationId, SAGA_TTL);
        stringRedisTemplate.opsForValue().set(SAGA_KEY_PREFIX + orderId + ":created_at", LocalDateTime.now().toString(), SAGA_TTL);

        // 3. Gửi lệnh giữ kho sang Catalog Service qua RabbitMQ (kèm correlationId và messageId)
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQOrderConfig.EXCHANGE_ORDER,
                    RabbitMQOrderConfig.ROUTING_KEY_ORDER_CREATED,
                    event,
                    message -> {
                        message.getMessageProperties().setCorrelationId(correlationId);
                        message.getMessageProperties().setMessageId(messageId);
                        message.getMessageProperties().setHeader("correlationId", correlationId);
                        message.getMessageProperties().setHeader("orderId", String.valueOf(orderId));
                        return message;
                    }
            );

            log.info("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Đã gửi thành công OrderCreatedEvent sang exchange '{}' với routingKey '{}'",
                    orderId, correlationId, RabbitMQOrderConfig.EXCHANGE_ORDER, RabbitMQOrderConfig.ROUTING_KEY_ORDER_CREATED);
        } catch (Exception ex) {
            log.error("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Lỗi khi gửi message sang RabbitMQ: {}",
                    orderId, correlationId, ex.getMessage(), ex);
            // Ghi nhận lỗi tạm thời hoặc để retry
            throw ex;
        }
    }

    @Override
    @Transactional
    public void handleStockReservedSuccess(StockReservedEvent event, String correlationId) {
        if (event == null || event.getOrderId() == null) {
            log.warn("[Saga Orchestrator] Bỏ qua StockReservedEvent không hợp lệ (null)");
            return;
        }

        Long orderId = parseOrderId(event.getOrderId());
        if (orderId == null) {
            throw new AmqpRejectAndDontRequeueException("Mã đơn hàng không hợp lệ trong StockReservedEvent: " + event.getOrderId());
        }

        // 1. Idempotency Check: Tránh xử lý lặp lại message trùng
        String idempotencyKey = SAGA_STOCK_RESERVED_KEY + orderId;
        Boolean isFirstProcessed = stringRedisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSED", SAGA_TTL);
        if (Boolean.FALSE.equals(isFirstProcessed)) {
            log.info("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Phát hiện message StockReservedEvent trùng lặp, bỏ qua an toàn",
                    orderId, correlationId);
            return;
        }

        log.info("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Tiếp nhận StockReservedEvent thành công từ Catalog Service",
                orderId, correlationId);

        // 2. Tra cứu Master Order trong CSDL
        MasterOrder masterOrder = masterOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("[Saga Orchestrator][OrderID: {}] Không tìm thấy đơn hàng trong CSDL để cập nhật giữ kho thành công", orderId);
                    return new AmqpRejectAndDontRequeueException("Đơn hàng không tồn tại trong CSDL: " + orderId);
                });

        // 3. Kiểm tra nếu đơn hàng đã bị hủy (ví dụ do timeout quá 15 phút đến trước message giữ kho)
        if (masterOrder.getStatus() == MasterOrderStatus.CANCELLED) {
            log.warn("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Đơn hàng đã ở trạng thái CANCELLED, gửi lệnh hoàn lại kho vừa giữ",
                    orderId, correlationId);
            sendReleaseStockCommand(orderId, "Đơn hàng đã bị hủy do timeout trước khi giữ kho thành công");
            return;
        }

        // 4. Cập nhật trạng thái Master Order sang PENDING_PAYMENT và đánh dấu giữ kho thành công
        masterOrder.setStatus(MasterOrderStatus.PENDING_PAYMENT);
        masterOrderRepository.save(masterOrder);

        stringRedisTemplate.opsForValue().set(SAGA_KEY_PREFIX + orderId + ":status", "STOCK_RESERVED", SAGA_TTL);
        stringRedisTemplate.opsForValue().set(SAGA_KEY_PREFIX + orderId + ":stock_reserved", "true", SAGA_TTL);

        log.info("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Bước 1 Giữ kho THÀNH CÔNG. Đơn hàng chuyển sang PENDING_PAYMENT, tiếp tục bước thanh toán.",
                orderId, correlationId);
    }

    @Override
    @Transactional
    public void handleStockReservedFailed(StockReservedEvent event, String correlationId) {
        if (event == null || event.getOrderId() == null) {
            log.warn("[Saga Orchestrator] Bỏ qua StockReservedEvent thất bại không hợp lệ (null)");
            return;
        }

        Long orderId = parseOrderId(event.getOrderId());
        if (orderId == null) {
            throw new AmqpRejectAndDontRequeueException("Mã đơn hàng không hợp lệ trong StockReservedEvent: " + event.getOrderId());
        }

        // 1. Idempotency Check
        String idempotencyKey = SAGA_STOCK_FAILED_KEY + orderId;
        Boolean isFirstProcessed = stringRedisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSED", SAGA_TTL);
        if (Boolean.FALSE.equals(isFirstProcessed)) {
            log.info("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Phát hiện message StockFailed trùng lặp, bỏ qua an toàn",
                    orderId, correlationId);
            return;
        }

        log.warn("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Nhận thông báo giữ kho THẤT BẠI từ Catalog Service (Lý do: {}). Kích hoạt Compensating Actions...",
                orderId, correlationId, event.getReason());

        // 2. Tra cứu Master Order trong CSDL
        MasterOrder masterOrder = masterOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("[Saga Orchestrator][OrderID: {}] Không tìm thấy đơn hàng trong CSDL để bù trừ", orderId);
                    return new AmqpRejectAndDontRequeueException("Đơn hàng không tồn tại trong CSDL: " + orderId);
                });

        // 3. Compensating Action 1: Cập nhật Master Order và các Sub-Orders sang CANCELLED
        if (masterOrder.getStatus() != MasterOrderStatus.CANCELLED) {
            masterOrder.setStatus(MasterOrderStatus.CANCELLED);
            if (masterOrder.getSubOrders() != null) {
                for (SubOrder subOrder : masterOrder.getSubOrders()) {
                    subOrder.setStatus(SubOrderStatus.CANCELLED);
                }
            }
            masterOrderRepository.save(masterOrder);
        }

        // 4. Compensating Action 2: Hoàn lại Voucher đã sử dụng (chỉ bù trừ những gì đã thành công)
        List<String> shopVoucherCodes = new ArrayList<>();
        if (masterOrder.getSubOrders() != null) {
            for (SubOrder subOrder : masterOrder.getSubOrders()) {
                if (subOrder.getShopVoucherCode() != null && !subOrder.getShopVoucherCode().isBlank()) {
                    shopVoucherCodes.add(subOrder.getShopVoucherCode());
                }
            }
        }
        voucherService.releaseVouchers(masterOrder.getVoucherCode(), shopVoucherCodes);

        // 5. Kho hàng: Do Catalog thông báo giữ kho THẤT BẠI, kho CHƯA từng được giữ,
        // TUYỆT ĐỐI KHÔNG gửi lệnh nhả kho (Tuân thủ nguyên tắc: Chỉ bù trừ những bước đã thành công).

        // 6. Phát sự kiện RollbackEvent thông báo toàn hệ thống
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .orderId(String.valueOf(orderId))
                .serviceName("order-service")
                .reason("Catalog stock reservation failed: " + event.getReason())
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQOrderConfig.EXCHANGE_ORDER,
                RabbitMQOrderConfig.ROUTING_KEY_ORDER_ROLLBACK,
                rollbackEvent
        );

        stringRedisTemplate.opsForValue().set(SAGA_KEY_PREFIX + orderId + ":status", "CANCELLED", SAGA_TTL);

        log.info("[Saga Orchestrator][OrderID: {}][CorrelationID: {}] Compensating Actions hoàn tất: Đơn hàng đã hủy, voucher đã được hoàn trả.",
                orderId, correlationId);
    }

    @Override
    @Transactional
    public int cancelExpiredOrders() {
        // Quét các đơn hàng PENDING_PAYMENT hoặc DRAFT được tạo quá 15 phút trước
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
        List<MasterOrder> expiredOrders = masterOrderRepository.findExpiredOrders(
                List.of(MasterOrderStatus.PENDING_PAYMENT, MasterOrderStatus.DRAFT),
                cutoffTime
        );

        if (expiredOrders.isEmpty()) {
            return 0;
        }

        log.info("[Saga Orchestrator] Bắt đầu quét đơn hàng quá hạn 15 phút chưa thanh toán. Tìm thấy {} đơn cần hủy",
                expiredOrders.size());

        int cancelledCount = 0;
        for (MasterOrder expiredOrder : expiredOrders) {
            try {
                Long orderId = expiredOrder.getId();

                // 1. Kiểm tra trạng thái mới nhất từ cơ sở dữ liệu để tránh race condition khi khách vừa thanh toán
                MasterOrder freshOrder = masterOrderRepository.findById(orderId).orElse(expiredOrder);

                // Không hủy đơn đã thanh toán (PROCESSING, COMPLETED, PARTIALLY_COMPLETED) hoặc đơn đã bị hủy trước đó
                if (freshOrder.getStatus() != MasterOrderStatus.PENDING_PAYMENT &&
                    freshOrder.getStatus() != MasterOrderStatus.DRAFT) {
                    log.info("[Saga Orchestrator][OrderID: {}] Bỏ qua hủy vì trạng thái hiện tại là {} (đơn đã thanh toán hoặc đã hoàn tất)",
                            orderId, freshOrder.getStatus());
                    continue;
                }

                log.info("[Saga Orchestrator][OrderID: {}] Đơn hàng hết hạn thanh toán 15 phút (tạo lúc: {}). Đang tiến hành hủy và bù trừ...",
                        orderId, freshOrder.getCreatedAt());

                // 2. Gửi lệnh Release Stock sang Catalog Service qua RabbitMQ
                sendReleaseStockCommand(orderId, "Hết hạn thanh toán 15 phút (Timeout Cancellation)");

                // 3. Cập nhật trạng thái CANCELLED cho Master Order và các Sub-Orders
                freshOrder.setStatus(MasterOrderStatus.CANCELLED);
                if (freshOrder.getSubOrders() != null) {
                    for (SubOrder subOrder : freshOrder.getSubOrders()) {
                        subOrder.setStatus(SubOrderStatus.CANCELLED);
                    }
                }
                masterOrderRepository.save(freshOrder);

                // 4. Hoàn lại Voucher đã áp dụng
                List<String> shopVoucherCodes = new ArrayList<>();
                if (freshOrder.getSubOrders() != null) {
                    for (SubOrder subOrder : freshOrder.getSubOrders()) {
                        if (subOrder.getShopVoucherCode() != null && !subOrder.getShopVoucherCode().isBlank()) {
                            shopVoucherCodes.add(subOrder.getShopVoucherCode());
                        }
                    }
                }
                voucherService.releaseVouchers(freshOrder.getVoucherCode(), shopVoucherCodes);

                stringRedisTemplate.opsForValue().set(SAGA_KEY_PREFIX + orderId + ":status", "TIMEOUT_CANCELLED", SAGA_TTL);
                cancelledCount++;
            } catch (Exception ex) {
                log.error("[Saga Orchestrator][OrderID: {}] Lỗi khi xử lý hủy đơn quá hạn: {}",
                        expiredOrder.getId(), ex.getMessage(), ex);
            }
        }

        log.info("[Saga Orchestrator] Hoàn thành tác vụ timeout: Đã hủy và bù trừ thành công {}/{} đơn hàng",
                cancelledCount, expiredOrders.size());
        return cancelledCount;
    }

    private void sendReleaseStockCommand(Long orderId, String reason) {
        RollbackEvent releaseStockEvent = RollbackEvent.builder()
                .orderId(String.valueOf(orderId))
                .serviceName("order-service")
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQOrderConfig.EXCHANGE_ORDER,
                RabbitMQOrderConfig.ROUTING_KEY_ORDER_CANCELLED,
                releaseStockEvent
        );

        log.info("[Saga Orchestrator][OrderID: {}] Đã gửi lệnh Release Stock sang exchange '{}' với routingKey '{}'",
                orderId, RabbitMQOrderConfig.EXCHANGE_ORDER, RabbitMQOrderConfig.ROUTING_KEY_ORDER_CANCELLED);
    }

    private Long parseOrderId(String orderIdStr) {
        try {
            return Long.parseLong(orderIdStr.trim());
        } catch (Exception ex) {
            log.error("[Saga Orchestrator] Không thể parse orderId '{}': {}", orderIdStr, ex.getMessage());
            return null;
        }
    }
}
