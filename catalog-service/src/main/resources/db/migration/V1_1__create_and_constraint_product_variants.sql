-- ====================================================================================
-- QuickShop Catalog Service Migration
-- File: V1_1__create_and_constraint_product_variants.sql
-- Mô tả: Cấu trúc bảng product_variants, chỉ mục và các ràng buộc kiểm tra không cho giá/tồn kho âm
-- Người phụ trách: Triệu Quốc Bình (2026-11-03)
-- ====================================================================================

CREATE TABLE IF NOT EXISTS product_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(50) NULL,
    size VARCHAR(20) NULL,
    price DECIMAL(12, 0) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    reserved_stock INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_product_variants_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_product_variants_stock_non_negative CHECK (stock >= 0),
    CONSTRAINT chk_product_variants_reserved_stock_non_negative CHECK (reserved_stock >= 0),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON product_variants(sku);

