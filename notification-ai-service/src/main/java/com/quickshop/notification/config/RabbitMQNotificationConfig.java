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

    public static final String EXCHANGE_NOTIFICATION = "notification.exchange";
    public static final String EXCHANGE_ORDER = "order.exchange";

    public static final String QUEUE_NOTIFICATION_EMAIL = "notification.email.queue";
    public static final String QUEUE_ORDER_NOTIFICATION = "notification.order.queue";

    public static final String ROUTING_KEY_EMAIL = "notification.email.#";
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE_NOTIFICATION);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    @Bean
    public Queue notificationEmailQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_EMAIL).build();
    }

    @Bean
    public Queue orderNotificationQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_NOTIFICATION).build();
    }

    @Bean
    public Binding notificationEmailBinding(Queue notificationEmailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationEmailQueue).to(notificationExchange).with(ROUTING_KEY_EMAIL);
    }

    @Bean
    public Binding orderNotificationBinding(Queue orderNotificationQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderNotificationQueue).to(orderExchange).with(ROUTING_KEY_ORDER_CREATED);
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
