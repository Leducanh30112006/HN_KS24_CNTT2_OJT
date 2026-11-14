# QuickShop - Sàn Thương Mại Điện Tử Đa Người Bán (Multi-Vendor Marketplace)

> **Dự án tốt nghiệp OJT:** Hệ thống TMĐT phân tán kiến trúc Microservices & Cloud-Native  
> **Nhóm thực hiện:** Nhóm 03 - PTIT CNTT2  
> **Thành viên:**  
> - **Lê Đức Anh** - DevOps Lead, API Gateway & Security / Observability  
> - **Nguyễn Văn Kiên** - Backend Lead, Saga Orchestration, Order, Payment & Shipping  
> - **Triệu Quốc Bình** - Frontend Lead, UI/UX Specialist, Buyer/Seller/Admin Integration  

---

## 1. Tổng quan Kiến trúc Hệ thống

QuickShop được xây dựng theo mô hình kiến trúc Microservices hoàn chỉnh, áp dụng các chuẩn thiết kế hiện đại trong thương mại điện tử:
- **API Gateway Pattern:** Định tuyến tập trung toàn bộ traffic qua Nginx và Spring Cloud Gateway (Port 8080), xử lý xác thực JWT tập trung, CORS và Rate Limiting.
- **Saga Orchestrator Pattern:** Điều phối giao dịch phân tán giữa kho hàng, voucher, đơn hàng và thanh toán không phong tỏa cơ sở dữ liệu (Non-blocking).
- **Event-Driven Architecture (EDA):** Tích hợp RabbitMQ để truyền thông tin bất đồng bộ, hỗ trợ Dead Letter Queue (DLQ) tự động phục hồi thông điệp lỗi.
- **Tài chính & Sổ cái kép (Double-entry Ledger):** Mô hình Escrow giữ tiền trung gian bảo vệ người mua, trích hoa hồng 5% sàn và quyết toán cho người bán sau 72h.
- **Vận chuyển thông minh:** Tích hợp tính phí giao hàng GHN/GHTK, sinh mã vận đơn tự động và xử lý Webhook Idempotency chống lặp sự kiện.
- **AI Recommendation:** Tích hợp Google Gemini API gợi ý sản phẩm thông minh và chatbot hỗ trợ khách hàng theo ngữ cảnh.

---

## 2. Bảng Danh mục Services & Cổng kết nối (Port Matrix)

| Phân hệ / Service | Công nghệ | Port Cục bộ | Container Port | Trách nhiệm chính |
| :--- | :--- | :--- | :--- | :--- |
| **Nginx Reverse Proxy** | Nginx Alpine | `80`, `443` | `80`, `443` | SSL Termination, routing `/api/` -> Gateway, `/` -> Frontend |
| **API Gateway** | Spring Cloud Gateway | `8080` | `8080` | Entry point, JWT filter, Redis token blacklist, rate limit |
| **Identity Service** | Spring Boot, Spring Security | `8081` | `8081` | Quản lý User, KYC Shop, RBAC (Buyer, Seller, Admin) |
| **Catalog Service** | Spring Boot, JPA, Redis | `8082` | `8082` | Danh mục đa cấp, sản phẩm, SKU, kiểm soát tồn kho lạc quan |
| **Order Service** | Spring Boot, Redis, RabbitMQ | `8083` | `8083` | Giỏ hàng Redis TTL, Multi-vendor Checkout, Saga Orchestrator, DLQ |
| **Payment Service** | Spring Boot, MySQL | `8084` | `8084` | VNPay Sandbox, tài khoản Escrow, ví người bán, Sổ cái kép |
| **Shipping Service** | Spring Boot, MySQL | `8085` | `8085` | GHN/GHTK Calculator, vận đơn, Webhook Idempotent, Simulator |
| **Notification AI Service** | Spring Boot, Gemini API | `8086` | `8086` | Gửi Email xác nhận đơn, AI Chatbot tư vấn, gợi ý sản phẩm |
| **Frontend Web App** | React 18, Vite, Tailwind | `5173` | `80` | Giao diện Buyer (Shopee Style), Seller Center, Admin Portal |
| **MySQL Database** | MySQL 8.0 | `3306` | `3306` | Lưu trữ dữ liệu phân vùng theo từng microservice qua `init.sql` |
| **Redis Cache** | Redis 7 Alpine | `6379` | `6379` | Cache sản phẩm, giỏ hàng người dùng, Token Blacklist |
| **RabbitMQ Broker** | RabbitMQ 3.13 Management | `5672`, `15672`| `5672`, `15672`| Message broker trao đổi sự kiện Saga, DLQ dashboard |
| **MinIO Storage** | S3-Compatible Object Store | `9010`, `9011` | `9000`, `9001` | Lưu trữ ảnh sản phẩm, banner và hồ sơ KYC (tránh xung đột cổng 9000) |
| **Prometheus** | Prometheus TSDB | `9090` | `9090` | Thu thập chỉ số hiệu năng (Metrics) qua Actuator |
| **Grafana** | Grafana Dashboard | `3000` | `3000` | Trực quan hóa số liệu hệ thống, giám sát tài nguyên |
| **Zipkin** | OpenZipkin Distributed Tracing| `9411` | `9411` | Phân tích độ trễ và luồng gọi chéo giữa các service |

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
- **Kênh Người Bán:** Chọn "Kênh Người Bán" trên thanh Header (hoặc đăng nhập tài khoản Seller)
- **Cổng Quản Trị Hệ Thống:** Chọn "Cổng Quản Trị" trên Header (tài khoản Admin)
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

## 4. Tài liệu Bàn giao & Báo cáo Đảm bảo Chất lượng
- [Hướng dẫn Triển khai VPS & CI/CD Pipeline (Deployment Guide)](docs/DEPLOYMENT_GUIDE.md)
- [Tài liệu Hướng dẫn Sử dụng (User Guide)](docs/USER_GUIDE.md)
- [Báo cáo Kiểm thử Backend (QA Backend Report)](docs/QA_BACKEND_TEST_REPORT.md)
- [Báo cáo Kiểm thử Frontend (QA Frontend Report)](docs/QA_FRONTEND_TEST_REPORT.md)
- [Báo cáo Hồi quy Toàn diện Backend (Backend Regression Report)](docs/BACKEND_REGRESSION_REPORT.md)
- [Danh sách Vấn đề Tồn đọng & Giải pháp (Known Issues)](docs/KNOWN_ISSUES.md)
- [Checklist Rà soát Bảo mật & An toàn Thông tin](docs/SECURITY_CHECKLIST.md)
- [Checklist Phát hành & Quy trình Bàn giao Hệ thống](docs/RELEASE_CHECKLIST.md)
- [Biên bản Bàn giao Dự án (Handover Report)](docs/HANDOVER_REPORT.md)

---
© 2026 QuickShop Team - PTIT CNTT2.
