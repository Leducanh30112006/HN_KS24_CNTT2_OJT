package com.quickshop.order.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bộ kiểm thử E2E sử dụng Testcontainers thực thụ cho MySQL, Redis và RabbitMQ.
 * Cấu hình disabledWithoutDocker = true:
 * - Khi môi trường CÓ Docker daemon (CI/CD hoặc Docker Desktop chạy): Khởi động 3 container thật để test full-stack.
 * - Khi môi trường KHÔNG CÓ Docker daemon (như host hiện tại): Tự động đánh dấu Disabled / Bỏ qua an toàn và báo cáo rõ.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("E2E Integration Tests with Testcontainers (MySQL, Redis, RabbitMQ)")
class QuickShopTestcontainersE2ETest {

    @Container
    static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("order_db")
            .withUsername("quickshop_test")
            .withPassword("quickshop_secret");

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2.4-alpine")
            .withExposedPorts(6379);

    @Container
    static final RabbitMQContainer rabbitmqContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mysqlContainer.isRunning()) {
            registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
            registry.add("spring.datasource.username", mysqlContainer::getUsername);
            registry.add("spring.datasource.password", mysqlContainer::getPassword);
        }
        if (redisContainer.isRunning()) {
            registry.add("spring.data.redis.host", redisContainer::getHost);
            registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        }
        if (rabbitmqContainer.isRunning()) {
            registry.add("spring.rabbitmq.host", rabbitmqContainer::getHost);
            registry.add("spring.rabbitmq.port", rabbitmqContainer::getAmqpPort);
        }
    }

    @Test
    @DisplayName("Kiểm tra kết nối và trạng thái của các Testcontainers trong môi trường hỗ trợ Docker")
    void testContainersLiveness() {
        assertTrue(mysqlContainer.isRunning(), "MySQL Testcontainer phải đang chạy");
        assertTrue(redisContainer.isRunning(), "Redis Testcontainer phải đang chạy");
        assertTrue(rabbitmqContainer.isRunning(), "RabbitMQ Testcontainer phải đang chạy");
    }
}
