package com.quickshop.identity.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQIdentityConfig {

    public static final String EXCHANGE_SHOP = "shop.exchange";
    public static final String ROUTING_KEY_SHOP_APPROVED = "shop.approved";
    public static final String ROUTING_KEY_SHOP_REJECTED = "shop.rejected";

    @Bean
    public TopicExchange shopExchange() {
        return new TopicExchange(EXCHANGE_SHOP);
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
