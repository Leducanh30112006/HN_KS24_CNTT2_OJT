-- QuickShop Multi-Database Initialization Script
CREATE DATABASE IF NOT EXISTS `identity_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `catalog_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `order_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON identity_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'root'@'%';

FLUSH PRIVILEGES;
