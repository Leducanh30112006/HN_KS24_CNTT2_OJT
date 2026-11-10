package com.quickshop.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQPaymentConfig {

    public static final String EXCHANGE_ORDER = "order.exchange";
    public static final String QUEUE_PAYMENT_REQUESTED = "order.payment.requested.queue";
    public static final String ROUTING_KEY_PAYMENT_REQUESTED = "payment.requested";
    public static final String ROUTING_KEY_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.failed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    @Bean
    public Queue paymentRequestedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_REQUESTED).build();
    }

    @Bean
    public Binding paymentRequestedBinding(Queue paymentRequestedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(paymentRequestedQueue).to(orderExchange).with(ROUTING_KEY_PAYMENT_REQUESTED);
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
