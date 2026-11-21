import fs from 'fs';
import path from 'path';
import { defaultProducts } from '../frontend/src/data/sampleProducts.js';

let sql = `-- Clean UTF-8 catalog fix
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. Update Existing Categories with clean UTF-8
UPDATE categories SET name = 'Thiết bị Điện tử' WHERE id = 1;
UPDATE categories SET name = 'Thời trang & Làm đẹp' WHERE id = 2;
UPDATE categories SET name = 'Đời sống & Gia đình' WHERE id = 3;
UPDATE categories SET name = 'Điện thoại & Phụ kiện' WHERE id = 4;
UPDATE categories SET name = 'Laptop & Máy tính' WHERE id = 5;
UPDATE categories SET name = 'Thời trang nam nữ' WHERE id = 6;
UPDATE categories SET name = 'Giày dép & Túi xách' WHERE id = 7;
UPDATE categories SET name = 'Mỹ phẩm & Làm đẹp' WHERE id = 8;
UPDATE categories SET name = 'Nhà cửa & Đời sống' WHERE id = 9;

-- Also support lookup by slug
UPDATE categories SET name = 'Thiết bị Điện tử' WHERE slug = 'thiet-bi-dien-tu';
UPDATE categories SET name = 'Thời trang & Làm đẹp' WHERE slug = 'thoi-trang-lam-dep';
UPDATE categories SET name = 'Đời sống & Gia đình' WHERE slug = 'doi-song-gia-dinh';
UPDATE categories SET name = 'Điện thoại & Phụ kiện' WHERE slug = 'dien-thoai-phu-kien';
UPDATE categories SET name = 'Laptop & Máy tính' WHERE slug = 'laptop-may-tinh';
UPDATE categories SET name = 'Thời trang nam nữ' WHERE slug = 'thoi-trang';
UPDATE categories SET name = 'Giày dép & Túi xách' WHERE slug = 'giay-dep-tui-xach';
UPDATE categories SET name = 'Mỹ phẩm & Làm đẹp' WHERE slug = 'my-pham-lam-dep';
UPDATE categories SET name = 'Nhà cửa & Đời sống' WHERE slug = 'nha-cua-doi-song';

-- 2. Update Products
`;

for (const p of defaultProducts) {
  const safeName = p.name.replace(/'/g, "''");
  const safeDesc = (p.description || '').replace(/'/g, "''");
  const safeImg = (p.mainImage || '').replace(/'/g, "''");
  const shopId = p.shopId || 1;
  const catId = p.categoryId || 1;
  const status = p.status || 'ACTIVE';

  // Update existing row
  sql += `UPDATE products SET name = '${safeName}', description = '${safeDesc}', main_image = '${safeImg}', status = '${status}' WHERE id = ${p.id};\n`;
  // Insert if not exists (for products 51-54)
  sql += `INSERT IGNORE INTO products (id, category_id, shop_id, name, description, main_image, status, created_at, updated_at)
VALUES (${p.id}, ${catId}, ${shopId}, '${safeName}', '${safeDesc}', '${safeImg}', '${status}', NOW(), NOW());\n`;

  // Variants
  if (Array.isArray(p.variants)) {
    for (const v of p.variants) {
      const safeColor = (v.color || '').replace(/'/g, "''");
      const safeSize = (v.size || '').replace(/'/g, "''");
      const price = v.price || p.minPrice || 100000;
      const stock = v.stock || 100;
      const sku = v.sku || `SKU-PROD-${p.id}-VAR-${v.id}`;

      sql += `UPDATE product_variants SET color = '${safeColor}', size = '${safeSize}', price = ${price}, stock = ${stock} WHERE id = ${v.id};\n`;
      sql += `INSERT IGNORE INTO product_variants (id, product_id, sku, color, size, price, stock, reserved_stock)
VALUES (${v.id}, ${p.id}, '${sku}', '${safeColor}', '${safeSize}', ${price}, ${stock}, 0);\n`;
    }
  }
}

// 3. Fix Product 64 and test variants
sql += `\n-- 3. Fix Product 64
UPDATE products SET name = 'Cáp Sạc Nhanh Type-C to Lightning Silicon Siêu Bền QuickShop', description = 'Cáp sạc nhanh silicon chịu nhiệt, độ bền cao, chống đứt gập', status = 'APPROVED' WHERE id = 64;
UPDATE product_variants SET color = 'Đen Carbon', size = 'Tiêu chuẩn 1m', price = 49000, stock = 100 WHERE product_id = 64;

-- Also fix any other test product
UPDATE products SET name = 'Bộ Vi Xử Lý Intel Core i5 14400F 10 Nhân 16 Luồng Socket LGA1700' WHERE id = 72;
`;

fs.writeFileSync(path.resolve('scripts/catalog_clean_utf8.sql'), sql, 'utf8');
console.log('Successfully generated clean UTF-8 SQL file');
