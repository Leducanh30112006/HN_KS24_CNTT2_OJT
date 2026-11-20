const path = require('path');
const puppeteer = require('puppeteer-core');
const fs = require('fs');

const outDir = path.resolve(__dirname, '../docs/images');
if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

const htmlContent = `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #0f172a; margin: 0; padding: 20px; color: #f8fafc; }
  .diagram-container { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 30px; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.5); width: 1200px; margin: 0 auto 40px auto; }
  h2 { margin-top: 0; color: #38bdf8; font-size: 24px; text-align: center; margin-bottom: 25px; text-transform: uppercase; letter-spacing: 1px; }
  
  /* Architecture Layout */
  .arch-grid { display: grid; grid-template-columns: 240px 1fr 240px; gap: 24px; align-items: center; }
  .arch-col { display: flex; flex-direction: column; gap: 14px; }
  .arch-card { background: #0f172a; border: 1px solid #475569; border-radius: 8px; padding: 14px; text-align: center; }
  .arch-card.client { border-color: #38bdf8; }
  .arch-card.gateway { border-color: #ec4899; background: #831843; }
  .arch-card.service { border-color: #10b981; }
  .arch-card.storage { border-color: #f59e0b; }
  .arch-card.monitoring { border-color: #a855f7; }
  .title { font-weight: bold; font-size: 15px; margin-bottom: 4px; }
  .desc { font-size: 12px; color: #94a3b8; }
  .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; margin-top: 6px; }
  .badge-blue { background: #0284c7; color: white; }
  .badge-green { background: #059669; color: white; }
  .badge-orange { background: #d97706; color: white; }
  .badge-purple { background: #7c3aed; color: white; }
  
  .services-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
</style>
</head>
<body>

<div id="arch-box" class="diagram-container">
  <h2>SƠ ĐỒ KIẾN TRÚC TỔNG THỂ MICROSERVICES QUICKSHOP (CLOUD-NATIVE)</h2>
  <div class="arch-grid">
    <!-- Clients -->
    <div class="arch-col">
      <div style="text-align: center; font-weight: bold; color: #38bdf8; margin-bottom: 4px;">4 PHÂN HỆ CLIENT</div>
      <div class="arch-card client">
        <div class="title" style="color:#38bdf8;">Buyer Portal</div>
        <div class="desc">React 18 / Shopee Style / GSAP / Lenis</div>
        <span class="badge badge-blue">Storefront</span>
      </div>
      <div class="arch-card client">
        <div class="title" style="color:#38bdf8;">Seller Center</div>
        <div class="desc">CKEditor 5, SKU Matrix, Doanh thu</div>
        <span class="badge badge-blue">Kênh Người Bán</span>
      </div>
      <div class="arch-card client">
        <div class="title" style="color:#38bdf8;">Admin Portal</div>
        <div class="desc">Duyệt KYC, Flash Sale AI, Sổ cái</div>
        <span class="badge badge-blue">Cổng Quản Trị</span>
      </div>
      <div class="arch-card client">
        <div class="title" style="color:#38bdf8;">Shipper Portal</div>
        <div class="desc">Bưu tá nhận đơn & cập nhật giao</div>
        <span class="badge badge-blue">Giao Hàng</span>
      </div>
    </div>

    <!-- Gateway & Microservices -->
    <div class="arch-col" style="gap: 18px;">
      <div class="arch-card gateway" style="padding: 16px;">
        <div class="title" style="font-size: 17px; color: #fff;">EDGE NGINX & SPRING CLOUD GATEWAY (PORT 8080)</div>
        <div class="desc" style="color: #fbcfe8;">SSL Termination | JWT Dual Auth Filter (Cookie/Bearer) | Token Bucket Rate Limiting | Nominatim Proxy | CORS</div>
      </div>

      <div style="text-align: center; font-weight: bold; color: #10b981; margin: 4px 0;">7 CORE MICROSERVICES (DATABASE-PER-SERVICE)</div>

      <div class="services-grid">
        <div class="arch-card service">
          <div class="title" style="color:#34d399;">Identity Service</div>
          <div class="desc">Port 8081 | Auth, Google OAuth, Email OTP, KYC Shop, RBAC</div>
          <span class="badge badge-green">identity_db</span>
        </div>
        <div class="arch-card service">
          <div class="title" style="color:#34d399;">Catalog Service</div>
          <div class="desc">Port 8082 | Danh mục, SKU, Đánh giá, MinIO CDN, CKEditor 5</div>
          <span class="badge badge-green">catalog_db</span>
        </div>
        <div class="arch-card service">
          <div class="title" style="color:#34d399;">Order Service</div>
          <div class="desc">Port 8083 | Giỏ hàng Redis, Multi-Shop Checkout, Saga Orchestrator</div>
          <span class="badge badge-green">order_db</span>
        </div>
        <div class="arch-card service">
          <div class="title" style="color:#34d399;">Payment Service</div>
          <div class="desc">Port 8084 | VNPay, PayOS VietQR, Sổ cái kép, Escrow 72h</div>
          <span class="badge badge-green">payment_db</span>
        </div>
        <div class="arch-card service">
          <div class="title" style="color:#34d399;">Shipping Service</div>
          <div class="desc">Port 8085 | GHN/GHTK, Webhook Idempotency, Kênh Shipper</div>
          <span class="badge badge-green">shipping_db</span>
        </div>
        <div class="arch-card service">
          <div class="title" style="color:#34d399;">Notification AI</div>
          <div class="desc">Port 8086 | RabbitMQ Email, Google Gemini AI Chatbot, SEO Generator</div>
          <span class="badge badge-green">notification_db</span>
        </div>
      </div>
    </div>

    <!-- Infrastructure & Storage -->
    <div class="arch-col">
      <div style="text-align: center; font-weight: bold; color: #f59e0b; margin-bottom: 4px;">HẠ TẦNG & LƯU TRỮ</div>
      <div class="arch-card storage">
        <div class="title" style="color:#fbbf24;">MySQL 8.0</div>
        <div class="desc">6 Isolated Databases, ACID Transaction, UTF8MB4</div>
        <span class="badge badge-orange">Port 3306</span>
      </div>
      <div class="arch-card storage">
        <div class="title" style="color:#fbbf24;">Redis 7 Alpine</div>
        <div class="desc">Cart Hash, Token Blacklist, OTP TTL, Cache</div>
        <span class="badge badge-orange">Port 6379</span>
      </div>
      <div class="arch-card storage">
        <div class="title" style="color:#fbbf24;">RabbitMQ 3.13</div>
        <div class="desc">Saga Message Exchange, Dead Letter Queue (order.dlq)</div>
        <span class="badge badge-orange">Port 5672</span>
      </div>
      <div class="arch-card storage">
        <div class="title" style="color:#fbbf24;">MinIO Object Storage</div>
        <div class="desc">S3 Compatible Storage, Media & Avatar Stream</div>
        <span class="badge badge-orange">Port 9010/9011</span>
      </div>
      <div class="arch-card monitoring">
        <div class="title" style="color:#c084fc;">Prometheus & Grafana</div>
        <div class="desc">TSDB Metrics, System Dashboards & Zipkin Tracing</div>
        <span class="badge badge-purple">Port 9090 / 3000</span>
      </div>
    </div>
  </div>
</div>

<div id="erd-box" class="diagram-container">
  <h2>SƠ ĐỒ THỰC THỂ LIÊN KẾT (ERD) CƠ SỞ DỮ LIỆU MICROSERVICES QUICKSHOP</h2>
  <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px;">
    <!-- Identity DB -->
    <div style="background: #0f172a; border: 1px solid #38bdf8; border-radius: 8px; padding: 14px;">
      <div style="color: #38bdf8; font-weight: bold; font-size: 15px; border-bottom: 1px solid #334155; padding-bottom: 6px; margin-bottom: 8px;">1. IDENTITY_DB</div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9;">users (Người dùng)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        • username, email (UNIQUE)<br>
        • password (BCrypt)<br>
        • role (BUYER, SELLER, ADMIN, SHIPPER)<br>
        • status (PENDING, ACTIVE, BLOCKED)
      </div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9; margin-top: 10px;">shops (Gian hàng)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        🔗 owner_id (Ref users.id)<br>
        • shop_name, tax_code, warehouse_address<br>
        • bank_code, bank_account_number<br>
        • status (PENDING_APPROVAL, ACTIVE)
      </div>
    </div>

    <!-- Catalog DB -->
    <div style="background: #0f172a; border: 1px solid #10b981; border-radius: 8px; padding: 14px;">
      <div style="color: #10b981; font-weight: bold; font-size: 15px; border-bottom: 1px solid #334155; padding-bottom: 6px; margin-bottom: 8px;">2. CATALOG_DB</div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9;">products (Sản phẩm)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        🔗 shop_id, category_id<br>
        • name, description (CKEditor 5 HTML)<br>
        • main_image (MinIO HTTPS URL)<br>
        • status (PENDING, ACTIVE, REJECTED)
      </div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9; margin-top: 10px;">product_variants (Biến thể SKU)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        🔗 product_id (FK products.id)<br>
        • sku (UNIQUE), color, size, price<br>
        • stock (Tồn kho thực tế)<br>
        • reserved_stock (Tồn kho đang giữ)
      </div>
    </div>

    <!-- Order DB -->
    <div style="background: #0f172a; border: 1px solid #f59e0b; border-radius: 8px; padding: 14px;">
      <div style="color: #f59e0b; font-weight: bold; font-size: 15px; border-bottom: 1px solid #334155; padding-bottom: 6px; margin-bottom: 8px;">3. ORDER_DB</div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9;">master_orders (Đơn tổng)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        🔗 user_id, total_amount, voucher_discount<br>
        • payment_method (COD, VNPAY, PAYOS_QR)<br>
        • status (DRAFT, PENDING, COMPLETED)
      </div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9; margin-top: 10px;">sub_orders (Đơn theo Shop)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        🔗 master_order_id, shop_id<br>
        • subtotal, shipping_fee, final_amount<br>
        • status (CONFIRMED, SHIPPING, DELIVERED)
      </div>
    </div>

    <!-- Payment DB -->
    <div style="background: #0f172a; border: 1px solid #ec4899; border-radius: 8px; padding: 14px;">
      <div style="color: #ec4899; font-weight: bold; font-size: 15px; border-bottom: 1px solid #334155; padding-bottom: 6px; margin-bottom: 8px;">4. PAYMENT_DB</div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9;">escrow_accounts (Ký quỹ Escrow)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        🔗 sub_order_id (UNIQUE)<br>
        • amount, commission_fee (5%)<br>
        • status (HOLDING, RELEASED, REFUNDED)
      </div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9; margin-top: 10px;">ledger_entries (Sổ cái kép)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK), transaction_id<br>
        • account_debit (Tài khoản Nợ)<br>
        • account_credit (Tài khoản Có)<br>
        • amount (VND) - Cân bằng Debit = Credit
      </div>
    </div>

    <!-- Shipping DB -->
    <div style="background: #0f172a; border: 1px solid #8b5cf6; border-radius: 8px; padding: 14px;">
      <div style="color: #8b5cf6; font-weight: bold; font-size: 15px; border-bottom: 1px solid #334155; padding-bottom: 6px; margin-bottom: 8px;">5. SHIPPING_DB</div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9;">shipments (Vận đơn giao hàng)</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        🔗 sub_order_id (UNIQUE)<br>
        • tracking_code (GHN-XXX, GHTK-XXX)<br>
        • carrier_name, shipping_fee<br>
        • shipping_status (READY_TO_PICK, DELIVERED)
      </div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9; margin-top: 10px;">shipping_webhooks_log</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        • event_id (UNIQUE - Idempotency)<br>
        • carrier_name, payload, processed_at
      </div>
    </div>

    <!-- Notification DB -->
    <div style="background: #0f172a; border: 1px solid #06b6d4; border-radius: 8px; padding: 14px;">
      <div style="color: #06b6d4; font-weight: bold; font-size: 15px; border-bottom: 1px solid #334155; padding-bottom: 6px; margin-bottom: 8px;">6. NOTIFICATION_DB</div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9;">notifications & email_logs</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        🔗 user_id, title, content, is_read<br>
        • recipient_email, subject, status<br>
        • sent_at (RabbitMQ Consumer)
      </div>
      <div style="font-size: 13px; font-weight: bold; color: #f1f5f9; margin-top: 10px;">flash_sales & ai_prompts</div>
      <div style="font-size: 12px; color: #94a3b8; line-height: 1.6;">
        🔑 <b>id</b> (BIGINT, PK)<br>
        • campaign_name, start_time, end_time<br>
        • is_ai_auto_approved (BOOLEAN)<br>
        • system_prompt_version (Sanitized)
      </div>
    </div>
  </div>
</div>

</body>
</html>`;

const tempHtmlPath = path.resolve(__dirname, 'temp_diagrams.html');
fs.writeFileSync(tempHtmlPath, htmlContent, 'utf8');

async function render() {
  console.log('Rendering diagrams via Chrome...');
  const browser = await puppeteer.launch({
    executablePath: chromePath,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1400,1600']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1350, height: 1600, deviceScaleFactor: 2 });
  await page.goto('file:///' + tempHtmlPath.replace(/\\/g, '/'), { waitUntil: 'load' });

  const archElement = await page.$('#arch-box');
  await archElement.screenshot({ path: path.join(outDir, 'architecture_diagram.png') });
  console.log('Saved architecture_diagram.png');

  const erdElement = await page.$('#erd-box');
  await erdElement.screenshot({ path: path.join(outDir, 'database_erd.png') });
  console.log('Saved database_erd.png');

  await browser.close();
  fs.unlinkSync(tempHtmlPath);
  console.log('Diagrams generation complete!');
}

render().catch(err => {
  console.error('Error rendering diagrams:', err);
  process.exit(1);
});
