package com.quickshop.order.producer;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.order.config.RabbitMQOrderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for orderId: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQOrderConfig.EXCHANGE_ORDER,
                RabbitMQOrderConfig.ROUTING_KEY_ORDER_CREATED,
                event
        );
    }
}
