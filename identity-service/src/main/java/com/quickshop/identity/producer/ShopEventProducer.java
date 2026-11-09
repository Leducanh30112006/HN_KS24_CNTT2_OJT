package com.quickshop.identity.producer;

import com.quickshop.common.event.ShopApprovedEvent;
import com.quickshop.common.event.ShopRejectedEvent;
import com.quickshop.identity.config.RabbitMQIdentityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishShopApproved(ShopApprovedEvent event) {
        log.info("Publishing ShopApprovedEvent for shopId: {}, ownerId: {}", event.getShopId(), event.getOwnerId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQIdentityConfig.EXCHANGE_SHOP,
                    RabbitMQIdentityConfig.ROUTING_KEY_SHOP_APPROVED,
                    event
            );
            log.info("Successfully published ShopApprovedEvent for shopId: {}", event.getShopId());
        } catch (AmqpException ex) {
            log.warn("Failed to publish ShopApprovedEvent to RabbitMQ (Broker might be offline): {}", ex.getMessage());
        }
    }

    public void publishShopRejected(ShopRejectedEvent event) {
        log.info("Publishing ShopRejectedEvent for shopId: {}, ownerId: {}", event.getShopId(), event.getOwnerId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQIdentityConfig.EXCHANGE_SHOP,
                    RabbitMQIdentityConfig.ROUTING_KEY_SHOP_REJECTED,
                    event
            );
            log.info("Successfully published ShopRejectedEvent for shopId: {}", event.getShopId());
        } catch (AmqpException ex) {
            log.warn("Failed to publish ShopRejectedEvent to RabbitMQ (Broker might be offline): {}", ex.getMessage());
        }
    }
}
