package com.quickshop.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQOrderConfig {

    public static final String EXCHANGE_ORDER = "order.exchange";
    public static final String EXCHANGE_CATALOG = "catalog.exchange";
    public static final String EXCHANGE_ORDER_DLX = "order.dlx";

    public static final String QUEUE_STOCK_RESERVED = "stock.reserved.queue";
    public static final String QUEUE_STOCK_FAILED = "stock.failed.queue";
    public static final String QUEUE_ORDER_DLQ = "order.dlq";

    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";
    public static final String ROUTING_KEY_STOCK_RESERVED = "stock.reserved";
    public static final String ROUTING_KEY_STOCK_FAILED = "stock.failed";
    public static final String ROUTING_KEY_ORDER_CANCELLED = "order.cancelled";
    public static final String ROUTING_KEY_ORDER_ROLLBACK = "order.rollback";
    public static final String ROUTING_KEY_DLQ = "order.dlq";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    @Bean
    public TopicExchange catalogExchange() {
        return new TopicExchange(EXCHANGE_CATALOG);
    }

    @Bean
    public TopicExchange orderDlxExchange() {
        return new TopicExchange(EXCHANGE_ORDER_DLX);
    }

    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(QUEUE_STOCK_RESERVED)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Queue stockFailedQueue() {
        return QueueBuilder.durable(QUEUE_STOCK_FAILED)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
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

    public static final String EXCHANGE_SHIPPING = "shipping.exchange";
    public static final String QUEUE_ORDER_SHIPPING = "order.shipping.status.queue";
    public static final String ROUTING_KEY_SHIPPING_STATUS_UPDATED = "shipping.status.updated";

    @Bean
    public TopicExchange shippingExchange() {
        return new TopicExchange(EXCHANGE_SHIPPING);
    }

    @Bean
    public Queue orderShippingStatusQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_SHIPPING).build();
    }

    @Bean
    public Binding orderShippingBinding(Queue orderShippingStatusQueue, TopicExchange shippingExchange) {
        return BindingBuilder.bind(orderShippingStatusQueue).to(shippingExchange).with(ROUTING_KEY_SHIPPING_STATUS_UPDATED);
    }

    @Bean
    public Binding orderDlqBinding(Queue orderDeadLetterQueue, TopicExchange orderDlxExchange) {
        return BindingBuilder.bind(orderDeadLetterQueue).to(orderDlxExchange).with("#");
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false);

        // Cấu hình Retry tối đa 3 lần với Exponential Backoff (1s -> 2s -> 4s)
        // Khi hết 3 lần retry, RejectAndDontRequeueRecoverer sẽ đẩy message vào DLQ order.dlq
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)
                        .backOffOptions(1000L, 2.0, 5000L)
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .build()
        );

        return factory;
    }
}
