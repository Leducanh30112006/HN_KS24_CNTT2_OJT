package com.quickshop.shipping.consumer;

import com.quickshop.common.event.OrderConfirmedEvent;
import com.quickshop.shipping.dto.CreateShipmentRequest;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.repository.ShipmentRepository;
import com.quickshop.shipping.service.ShippingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingOrderEventConsumerTest {

    @Mock
    private ShippingService shippingService;

    @Mock
    private ShipmentRepository shipmentRepository;

    @InjectMocks
    private ShippingOrderEventConsumer consumer;

    @Test
    @DisplayName("handleOrderConfirmed: Nhận sự kiện đơn hàng có 2 SubOrder -> Tự động khởi tạo 2 vận đơn")
    void testHandleOrderConfirmed_Success() {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .masterOrderId(888L)
                .userId(101L)
                .subOrderIds(List.of(1001L, 1002L))
                .totalAmount(BigDecimal.valueOf(500000))
                .paymentMethod("VNPAY")
                .build();

        when(shipmentRepository.findBySubOrderId(1001L)).thenReturn(Optional.empty());
        when(shipmentRepository.findBySubOrderId(1002L)).thenReturn(Optional.empty());

        Shipment shipment1 = Shipment.builder().id(1L).subOrderId(1001L).trackingCode("GHN-AAA111").carrierName("GHN").build();
        Shipment shipment2 = Shipment.builder().id(2L).subOrderId(1002L).trackingCode("GHN-BBB222").carrierName("GHN").build();

        when(shippingService.createShipment(any(CreateShipmentRequest.class)))
                .thenReturn(shipment1)
                .thenReturn(shipment2);

        consumer.handleOrderConfirmed(event);

        ArgumentCaptor<CreateShipmentRequest> captor = ArgumentCaptor.forClass(CreateShipmentRequest.class);
        verify(shippingService, times(2)).createShipment(captor.capture());

        List<CreateShipmentRequest> requests = captor.getAllValues();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getSubOrderId()).isEqualTo(1001L);
        assertThat(requests.get(0).getCarrierName()).isEqualTo("GHN");
        assertThat(requests.get(1).getSubOrderId()).isEqualTo(1002L);
        assertThat(requests.get(1).getCarrierName()).isEqualTo("GHN");
    }

    @Test
    @DisplayName("handleOrderConfirmed: SubOrder đã có vận đơn -> Bỏ qua không tạo trùng")
    void testHandleOrderConfirmed_SkipExisting() {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .masterOrderId(999L)
                .userId(102L)
                .subOrderIds(List.of(2001L))
                .build();

        Shipment existing = Shipment.builder().id(5L).subOrderId(2001L).trackingCode("GHN-EXIST").build();
        when(shipmentRepository.findBySubOrderId(2001L)).thenReturn(Optional.of(existing));

        consumer.handleOrderConfirmed(event);

        verify(shippingService, never()).createShipment(any());
    }

    @Test
    @DisplayName("handleOrderConfirmed: Nhận event null hoặc rỗng -> Bỏ qua an toàn không throw exception")
    void testHandleOrderConfirmed_NullOrEmpty() {
        consumer.handleOrderConfirmed(null);
        verify(shippingService, never()).createShipment(any());

        OrderConfirmedEvent emptyEvent = OrderConfirmedEvent.builder()
                .masterOrderId(111L)
                .subOrderIds(List.of())
                .build();
        consumer.handleOrderConfirmed(emptyEvent);
        verify(shippingService, never()).createShipment(any());
    }
}

