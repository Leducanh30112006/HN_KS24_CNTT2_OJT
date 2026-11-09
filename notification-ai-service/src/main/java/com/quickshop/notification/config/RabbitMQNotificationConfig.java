package com.quickshop.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQNotificationConfig {

    public static final String EXCHANGE_ORDER = "order.exchange";
    public static final String EXCHANGE_SHOP = "shop.exchange";
    public static final String EXCHANGE_NOTIFICATION_DLX = "notification.dlx.exchange";

    public static final String QUEUE_NOTIFICATION_ORDER_CREATED = "notification.order.created.queue";
    public static final String QUEUE_NOTIFICATION_SHOP_APPROVED = "notification.shop.approved.queue";
    public static final String QUEUE_NOTIFICATION_SHOP_REJECTED = "notification.shop.rejected.queue";
    public static final String QUEUE_NOTIFICATION_DLQ = "notification.dlq";

    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";
    public static final String ROUTING_KEY_SHOP_APPROVED = "shop.approved";
    public static final String ROUTING_KEY_SHOP_REJECTED = "shop.rejected";
    public static final String ROUTING_KEY_NOTIFICATION_DLQ = "notification.dlq.routing";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    @Bean
    public TopicExchange shopExchange() {
        return new TopicExchange(EXCHANGE_SHOP);
    }

    @Bean
    public DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(EXCHANGE_NOTIFICATION_DLX);
    }

    @Bean
    public Queue notificationOrderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_ORDER_CREATED)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationShopApprovedQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_SHOP_APPROVED)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationShopRejectedQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_SHOP_REJECTED)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding notificationOrderCreatedBinding(Queue notificationOrderCreatedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationOrderCreatedQueue).to(orderExchange).with(ROUTING_KEY_ORDER_CREATED);
    }

    @Bean
    public Binding notificationShopApprovedBinding(Queue notificationShopApprovedQueue, TopicExchange shopExchange) {
        return BindingBuilder.bind(notificationShopApprovedQueue).to(shopExchange).with(ROUTING_KEY_SHOP_APPROVED);
    }

    @Bean
    public Binding notificationShopRejectedBinding(Queue notificationShopRejectedQueue, TopicExchange shopExchange) {
        return BindingBuilder.bind(notificationShopRejectedQueue).to(shopExchange).with(ROUTING_KEY_SHOP_REJECTED);
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDlq, DirectExchange notificationDeadLetterExchange) {
        return BindingBuilder.bind(notificationDlq).to(notificationDeadLetterExchange).with(ROUTING_KEY_NOTIFICATION_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
