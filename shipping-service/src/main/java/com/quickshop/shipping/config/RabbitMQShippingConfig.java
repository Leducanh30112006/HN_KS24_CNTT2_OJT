package com.quickshop.shipping.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQShippingConfig {

    public static final String EXCHANGE_ORDER = "order.exchange";
    public static final String QUEUE_SHIPPING_ORDER_CONFIRMED = "shipping.order.confirmed.queue";
    public static final String ROUTING_KEY_ORDER_CONFIRMED = "order.confirmed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    @Bean
    public Queue shippingOrderConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_SHIPPING_ORDER_CONFIRMED).build();
    }

    @Bean
    public Binding shippingOrderConfirmedBinding(Queue shippingOrderConfirmedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(shippingOrderConfirmedQueue).to(orderExchange).with(ROUTING_KEY_ORDER_CONFIRMED);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}

