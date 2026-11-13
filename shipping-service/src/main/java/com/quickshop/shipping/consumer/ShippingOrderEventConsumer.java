package com.quickshop.shipping.consumer;

import com.quickshop.common.event.OrderConfirmedEvent;
import com.quickshop.shipping.config.RabbitMQShippingConfig;
import com.quickshop.shipping.dto.CreateShipmentRequest;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.repository.ShipmentRepository;
import com.quickshop.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingOrderEventConsumer {

    private final ShippingService shippingService;
    private final ShipmentRepository shipmentRepository;

    @RabbitListener(queues = RabbitMQShippingConfig.QUEUE_SHIPPING_ORDER_CONFIRMED)
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        if (event == null) {
            log.warn("Nhận thông điệp OrderConfirmedEvent null trong Shipping Service, bỏ qua");
            return;
        }

        log.info("Shipping Service: Nhận OrderConfirmedEvent cho đơn hàng MasterOrder #{}, danh sách SubOrder: {}",
                event.getMasterOrderId(), event.getSubOrderIds());

        List<Long> subOrderIds = event.getSubOrderIds();
        if (subOrderIds == null || subOrderIds.isEmpty()) {
            log.info("Không có SubOrder nào cần tạo vận đơn cho đơn hàng #{}", event.getMasterOrderId());
            return;
        }

        for (Long subOrderId : subOrderIds) {
            try {
                if (shipmentRepository.findBySubOrderId(subOrderId).isPresent()) {
                    log.info("Vận đơn cho SubOrder #{} đã tồn tại, bỏ qua tạo mới", subOrderId);
                    continue;
                }

                CreateShipmentRequest request = CreateShipmentRequest.builder()
                        .subOrderId(subOrderId)
                        .carrierName("GHN")
                        .shippingFee(BigDecimal.valueOf(25000))
                        .recipientName("Khách hàng #" + (event.getUserId() != null ? event.getUserId() : "Online"))
                        .recipientPhone("0987654321")
                        .fromAddress("Kho Tổng QuickShop Logistics, TP. Hà Nội")
                        .toAddress("Địa chỉ người nhận theo đơn hàng #" + event.getMasterOrderId())
                        .weightGrams(500)
                        .build();

                Shipment shipment = shippingService.createShipment(request);
                log.info("Đã tự động khởi tạo vận đơn cho SubOrder #{}: trackingCode={}, carrier={}",
                        subOrderId, shipment.getTrackingCode(), shipment.getCarrierName());
            } catch (Exception e) {
                log.error("Lỗi khi tự động tạo vận đơn cho SubOrder #{}: {}", subOrderId, e.getMessage(), e);
            }
        }
    }
}

