package com.quickshop.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQOrderConfig {

    public static final String EXCHANGE_ORDER = "order.exchange";
    public static final String EXCHANGE_CATALOG = "catalog.exchange";
    public static final String EXCHANGE_ORDER_DLX = "order.dlx.exchange";

    public static final String QUEUE_STOCK_RESERVED = "stock.reserved.queue";
    public static final String QUEUE_STOCK_FAILED = "stock.failed.queue";
    public static final String QUEUE_ORDER_DLQ = "order.dlq";

    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";
    public static final String ROUTING_KEY_STOCK_RESERVED = "stock.reserved";
    public static final String ROUTING_KEY_STOCK_FAILED = "stock.failed";
    public static final String ROUTING_KEY_ORDER_DLQ = "order.dlq.routing";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    @Bean
    public TopicExchange catalogExchange() {
        return new TopicExchange(EXCHANGE_CATALOG);
    }

    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return new DirectExchange(EXCHANGE_ORDER_DLX);
    }

    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(QUEUE_STOCK_RESERVED).build();
    }

    @Bean
    public Queue stockFailedQueue() {
        return QueueBuilder.durable(QUEUE_STOCK_FAILED).build();
    }

    @Bean
    public Queue orderDeadLetterQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_DLQ).build();
    }

    @Bean
    public Binding stockReservedBinding(Queue stockReservedQueue, TopicExchange catalogExchange) {
        return BindingBuilder.bind(stockReservedQueue).to(catalogExchange).with(ROUTING_KEY_STOCK_RESERVED);
    }

    @Bean
    public Binding stockFailedBinding(Queue stockFailedQueue, TopicExchange catalogExchange) {
        return BindingBuilder.bind(stockFailedQueue).to(catalogExchange).with(ROUTING_KEY_STOCK_FAILED);
    }

    @Bean
    public Binding orderDlqBinding(Queue orderDeadLetterQueue, DirectExchange orderDeadLetterExchange) {
        return BindingBuilder.bind(orderDeadLetterQueue).to(orderDeadLetterExchange).with(ROUTING_KEY_ORDER_DLQ);
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
