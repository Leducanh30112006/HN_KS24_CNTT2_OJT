# QuickShop - Sàn Thương Mại Điện Tử Đa Người Bán (Multi-Vendor Marketplace)

> **Dự án tốt nghiệp OJT:** Hệ thống TMĐT phân tán kiến trúc Microservices & Cloud-Native  
> **Thời gian thực hiện:** 26/10/2026 – 20/11/2026 (Đúng **4 Sprint** – 20 ngày làm việc, nghỉ Thứ 7 và Chủ Nhật)  
> **Ngày hoàn thành & bàn giao:** **20/11/2026**  
> **Nhóm thực hiện:** Nhóm 03 - PTIT CNTT2  
> **Thành viên:**  
> - **Lê Đức Anh** - DevOps Lead, API Gateway & Security / Observability / AI  
> - **Nguyễn Văn Kiên** - Backend Lead, Saga Orchestration, Order, Payment & Shipping  
> - **Triệu Quốc Bình** - Frontend Lead, UI/UX Specialist, Storefront & Multi-Portal Integration  

---

## 1. Tổng quan Kiến trúc Hệ thống

QuickShop được xây dựng theo mô hình kiến trúc Microservices phân tán hiện đại, đáp ứng tiêu chuẩn sàn thương mại điện tử quy mô lớn:
- **API Gateway Pattern:** Định tuyến tập trung toàn bộ traffic qua Nginx và Spring Cloud Gateway (Port 8080), xử lý xác thực JWT Dual Auth (Cookie & Bearer), Rate Limiting, CORS và Correlation ID truy vết.
- **Saga Orchestrator Pattern:** Điều phối giao dịch phân tán giữa kho hàng, voucher, đơn hàng và thanh toán không phong tỏa cơ sở dữ liệu (Non-blocking) qua RabbitMQ.
- **Event-Driven Architecture (EDA):** Tích hợp RabbitMQ để truyền thông điệp bất đồng bộ, hỗ trợ Dead Letter Queue (`order.dlq`) tự động phục hồi lỗi.
- **Tài chính & Sổ cái kép (Double-entry Ledger):** Mô hình Escrow giữ tiền trung gian bảo vệ người mua, trích hoa hồng sàn 5% và tự động quyết toán giải phóng số dư ví người bán sau 72h hoàn tất đơn.
- **Thanh toán đa kênh:** Tích hợp Cổng thanh toán VNPay Sandbox và cổng chuyển khoản tự động PayOS chuẩn VietQR EMVCo dynamic QR code.
- **Vận chuyển thông minh & Kênh Shipper:** Tính phí giao hàng linh hoạt GHN/GHTK, sinh mã vận đơn tự động, xử lý Webhook Idempotency theo `event_id` và phân hệ dành riêng cho Nhân viên giao hàng (`ROLE_SHIPPER`).
- **Bản đồ số định vị GPS:** Tích hợp Leaflet OpenStreetMap và Nominatim Geocoding hỗ trợ chọn địa chỉ giao nhận hàng trực quan kèm Nginx Tile Proxy.
- **AI Recommendation & Chatbot:** Tích hợp Google Gemini API tư vấn mua sắm theo ngữ cảnh, Function Calling tra cứu sản phẩm trực tiếp và công cụ tự động sinh mô tả SEO cho Seller.
- **Giao tiếp thời gian thực:** Hệ thống tin nhắn Full-page Messenger giữa Người mua và Gian hàng nhúng Thẻ sản phẩm thông minh.
- **Trải nghiệm Flagship Impeccable:** Trang giới thiệu tương tác 3D Scrollytelling (WebGL/Three.js), thư viện cuộn mượt Lenis và chuyển động GSAP mượt mà.

---

## 2. Bảng Danh mục Services & Cổng kết nối (Port Matrix)

| Phân hệ / Service | Công nghệ | Port Cục bộ | Container Port | Trách nhiệm chính |
| :--- | :--- | :--- | :--- | :--- |
| **Nginx Reverse Proxy** | Nginx Alpine | `80`, `443` | `80`, `443` | SSL Termination, routing `/api/` -> Gateway, `/` -> Frontend, Tile/Geocoding Proxy |
| **API Gateway** | Spring Cloud Gateway | `8080` | `8080` | Entry point, JWT filter, Redis token blacklist, rate limit, CORS |
| **Identity Service** | Spring Boot, Spring Security | `8081` | `8081` | User, Google OAuth 2.0, OTP email Redis, KYC Shop, RBAC (Buyer, Seller, Admin, Shipper) |
| **Catalog Service** | Spring Boot, JPA, Redis | `8082` | `8082` | Danh mục đa cấp, sản phẩm đa biến thể SKU, khóa lạc quan, upload ảnh MinIO, CKEditor 5 |
| **Order Service** | Spring Boot, Redis, RabbitMQ | `8083` | `8083` | Giỏ hàng Redis TTL, Multi-vendor Checkout, Saga Orchestrator, DLQ, Hủy đơn quá hạn |
| **Payment Service** | Spring Boot, MySQL | `8084` | `8084` | VNPay Sandbox, PayOS VietQR, Sổ cái kép (Ledger), Escrow Holding, Ví người bán |
| **Shipping Service** | Spring Boot, MySQL | `8085` | `8085` | GHN/GHTK Calculator, vận đơn, Webhook Idempotent, Simulator, Kênh Shipper |
| **Notification AI Service** | Spring Boot, Gemini API | `8086` | `8086` | Gửi Email hóa đơn tự động qua RabbitMQ, AI Chatbot tư vấn, AI SEO Generator |
| **Frontend Web App** | React 18, Vite, Tailwind | `5173` | `80` | 4 Phân hệ: Buyer Portal, Seller Center, Admin Portal, Shipper Portal & 3D Showcase |
| **MySQL Database** | MySQL 8.0 | `3306` | `3306` | Lưu trữ dữ liệu phân vùng theo từng microservice qua `init.sql` |
| **Redis Cache** | Redis 7 Alpine | `6379` | `6379` | Cache sản phẩm, giỏ hàng người dùng, OTP kích hoạt, Token Blacklist |
| **RabbitMQ Broker** | RabbitMQ 3.13 Management | `5672`, `15672`| `5672`, `15672`| Message broker trao đổi sự kiện Saga, DLQ dashboard |
| **MinIO Storage** | S3-Compatible Object Store | `9010`, `9011` | `9000`, `9001` | Lưu trữ ảnh sản phẩm, avatar, CKEditor inline images (Console cổng 9011) |
| **Prometheus** | Prometheus TSDB | `9090` | `9090` | Thu thập chỉ số hiệu năng (Metrics) qua Spring Actuator |
| **Grafana** | Grafana Dashboard | `3000` | `3000` | Trực quan hóa số liệu hệ thống, giám sát tài nguyên |
| **Zipkin** | OpenZipkin Distributed Tracing| `9411` | `9411` | Phân tích độ trễ và luồng gọi chéo giữa các microservices |

---

## 3. Hướng dẫn Cài đặt & Vận hành

### 3.1. Yêu cầu môi trường
- **Java:** JDK 17 (Eclipse Temurin khuyến nghị)
- **Node.js:** Node 20.x hoặc mới hơn, npm 10+
- **Docker:** Docker Desktop 4.x+ và Docker Compose v2.x+

### 3.2. Cấu hình biến môi trường
Sao chép tệp mẫu sang `.env`:
```bash
cp .env.example .env
```
Tùy chỉnh các thông số trong `.env` nếu cần (thông số mặc định đã được cấu hình sẵn sàng chạy ngay).

### 3.3. Khởi chạy toàn bộ hệ thống bằng Docker Compose (Khuyến nghị)
Khởi động hạ tầng, các microservices, frontend và hệ thống monitoring chỉ với một lệnh:
```bash
docker-compose -f docker-compose.prod.yml up -d --build
```
Kiểm tra trạng thái các container:
```bash
docker-compose -f docker-compose.prod.yml ps
```

Truy cập hệ thống:
- **Trang chủ Mua sắm (Frontend):** `http://localhost` (hoặc `http://localhost:5173` khi chạy dev)
- **Kênh Người Bán:** Chọn "Kênh Người Bán" trên thanh Header (tài khoản: `seller@techworld.vn` / `123456`)
- **Cổng Quản Trị Hệ Thống:** Chọn "Cổng Quản Trị" trên Header (tài khoản: `admin@quickshop.vn` / `admin123`)
- **Kênh Nhân viên Giao Hàng:** Đăng nhập bưu tá (tài khoản: `shipper@quickshop.vn` / `123456`)
- **Trang Giới thiệu 3D Scrollytelling:** Truy cập `/intro`
- **Hệ thống Tin nhắn Thời gian thực:** Truy cập `/messenger` hoặc bấm icon Chat ở góc dưới màn hình
- **RabbitMQ Dashboard:** `http://localhost:15672` (User: `quickshop` / Pass: `quickshop123`)
- **MinIO Storage Console:** `http://localhost:9011` (User: `minioadmin` / Pass: `minioadmin`)
- **Prometheus Dashboard:** `http://localhost:9090`
- **Grafana Dashboard:** `http://localhost:3000` (User: `admin` / Pass: `quickshop123`)
- **Zipkin Tracing:** `http://localhost:9411`

### 3.4. Chạy kiểm thử tự động (CI Quality Gate)
Chạy toàn bộ unit test & integration test của các microservices backend:
```bash
./gradlew test
```
Kiểm tra bản build frontend:
```bash
cd frontend && npm install && npm run build
```

---

## 4. Tài liệu Kế hoạch, Bàn giao & Báo cáo Đảm bảo Chất lượng
- [**Báo cáo Kế hoạch Dự án & Phân công Công việc 4 Sprint**](docs/PROJECT_PLAN_AND_WORK_BREAKDOWN_4_SPRINTS.md) ⭐
- [**File Import Phân công Công việc Trello 4 Sprint (60 Tasks)**](QuickShop_Trello_Import_Updated.csv) ⭐
- [Biên bản Bàn giao Dự án & Phân công Bảo trì (Handover Report - 20/11/2026)](docs/HANDOVER_REPORT.md)
- [Hướng dẫn Triển khai VPS & CI/CD Pipeline (Deployment Guide)](docs/DEPLOYMENT_GUIDE.md)
- [Tài liệu Hướng dẫn Sử dụng 4 Phân hệ (User Guide)](docs/USER_GUIDE.md)
- [Báo cáo Kiểm thử Tích hợp Backend (QA Backend Report)](docs/QA_BACKEND_TEST_REPORT.md)
- [Báo cáo Kiểm thử Giao diện Frontend (QA Frontend Report)](docs/QA_FRONTEND_TEST_REPORT.md)
- [Báo cáo Hồi quy Toàn diện Backend (Backend Regression Report)](docs/BACKEND_REGRESSION_REPORT.md)
- [Danh sách Vấn đề Tồn đọng & Giải pháp (Known Issues)](docs/KNOWN_ISSUES.md)
- [Checklist Rà soát Bảo mật & An toàn Thông tin (Security Checklist)](docs/SECURITY_CHECKLIST.md)
- [Checklist Phát hành & Quy trình Bàn giao Hệ thống (Release Checklist)](docs/RELEASE_CHECKLIST.md)

---
© 2026 QuickShop Team - PTIT CNTT2.
