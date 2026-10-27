-- QuickShop Multi-Database Initialization Script
CREATE DATABASE IF NOT EXISTS `identity_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `catalog_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `order_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `payment_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `shipping_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `notification_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON identity_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON shipping_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'root'@'%';

FLUSH PRIVILEGES;

-- ==========================================================
-- SHIPPING & LOGISTICS DATABASE SCHEMA (shipping_db)
-- SHIPPING & LOGISTICS DATABASE SCHEMA (notification_db)
-- ==========================================================
USE `shipping_db`;
USE `notification_db`;

CREATE TABLE IF NOT EXISTS `shipments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sub_order_id` BIGINT NOT NULL,
    `shop_id` BIGINT NOT NULL,
    `tracking_code` VARCHAR(50) NOT NULL,
    `carrier_name` VARCHAR(50) NOT NULL,
    `shipping_fee` DECIMAL(12,0) NOT NULL DEFAULT 0,
    `shipping_status` VARCHAR(50) NOT NULL,
    `recipient_name` VARCHAR(100),
    `recipient_phone` VARCHAR(20),
    `shipping_address` TEXT NOT NULL,
    `origin_address` TEXT,
    `weight_grams` INT NOT NULL DEFAULT 500,
    `cod_amount` DECIMAL(12,0) NOT NULL DEFAULT 0,
    `is_mock` BOOLEAN NOT NULL DEFAULT FALSE,
    `status_note` VARCHAR(255),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_shipments_sub_order_id` UNIQUE (`sub_order_id`),
    CONSTRAINT `uk_shipments_tracking_code` UNIQUE (`tracking_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `shipping_webhooks_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` VARCHAR(100) NOT NULL,
    `carrier_name` VARCHAR(50) NOT NULL,
    `payload` TEXT NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    `error_message` VARCHAR(255),
    `processed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uk_shipping_webhooks_event_id` UNIQUE (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
