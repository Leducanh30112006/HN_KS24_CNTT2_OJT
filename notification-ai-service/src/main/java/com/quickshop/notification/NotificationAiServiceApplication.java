package com.quickshop.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NotificationAiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationAiServiceApplication.class, args);
    }
}
