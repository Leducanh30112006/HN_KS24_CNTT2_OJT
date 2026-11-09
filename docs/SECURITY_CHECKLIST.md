# CHECKLIST RÀ SOÁT BẢO MẬT & GIÁM SÁT HỆ THỐNG (SECURITY & OBSERVABILITY)

**Người phụ trách:** Lê Đức Anh (DevOps & Security / Observability Lead)  
**Ngày thực hiện:** 14/11/2026 - 16/11/2026  
**Phân hệ:** DevOps, Security & Observability  

---

## 1. Rà soát An toàn Thông tin & Bảo mật (Security Review)

| Hạng mục kiểm tra | Tiêu chuẩn đánh giá | Hiện trạng tại QuickShop | Kết quả |
| :--- | :--- | :--- | :--- |
| **Bảo mật Bí mật (Secrets Management)** | Không hardcode mật khẩu, private key, token vào mã nguồn git | Tách biệt toàn bộ biến nhạy cảm vào `.env`, GitHub Secrets và docker-compose env | **ĐẠT** |
| **Cấu hình CORS (Cross-Origin)** | Giới hạn domain tin cậy, không mở wildcard `*` kèm credentials | Cấu hình tại API Gateway và Nginx, chỉ cho phép origin từ frontend portal | **ĐẠT** |
| **Chống Brute-force & DoS (Rate Limiting)** | Giới hạn tần suất request theo IP/User Token | Áp dụng Redis Token Bucket Filter tại Gateway cho các route nhạy cảm (`/auth/login`, `/checkout`) | **ĐẠT** |
| **Rà soát Log nhạy cảm (PII / Sensitive Logs)** | Không in ra mật khẩu, mã OTP, số thẻ tín dụng, chữ ký bí mật trong log | Sử dụng DTO filter, mask dữ liệu nhạy cảm trước khi log ra console và file | **ĐẠT** |
| **Phân quyền truy cập (RBAC Enforcement)** | Chặn truy cập trái quyền vào Seller Center và Admin Portal | Kiểm tra JWT Claims ở API Gateway và `@PreAuthorize` ở controller tầng microservice | **ĐẠT** |
| **Xác thực ký quỹ & Chống giả mạo** | Chữ ký số mã hóa callback thanh toán VNPay và Idempotent Webhook | Kiểm tra mã SHA512 hash của VNPay và `existsByEventId` chống lặp sự kiện webhook | **ĐẠT** |

---

## 2. Hệ thống Giám sát & Quan sát (Observability Architecture)

1. **Prometheus TSDB (Port 9090):**
   - Thu thập chỉ số định kỳ mỗi 15 giây từ Spring Boot Actuator (`/actuator/prometheus`).
   - Giám sát CPU Usage, Heap Memory, Connection Pool (HikariCP), HTTP Request Duration, GC Pause Time.
2. **Grafana Dashboards (Port 3000):**
   - Tự động cấu hình Datasource trỏ về Prometheus (`deployments/grafana/provisioning/datasources/datasources.yml`).
   - Cung cấp biểu đồ trực quan về lưu lượng truy cập (RPS), tỉ lệ lỗi 5xx/4xx và độ trễ phản hồi (p95, p99).
3. **OpenZipkin Distributed Tracing (Port 9411):**
   - Theo dõi dấu vết phân tán (Trace ID / Span ID) xuyên suốt qua API Gateway -> Order Service -> Catalog Service -> Payment Service.
   - Hỗ trợ phát hiện nhanh nút thắt cổ chai (Bottlenecks) và điểm nghẽn mạng giữa các container.
4. **Health Check Probes:**
   - Docker container healthchecks kiểm tra trạng thái sống của MySQL (`mysqladmin ping`), Redis (`redis-cli ping`), RabbitMQ (`rabbitmq-diagnostics ping`).

---

## 3. Kết luận Đảm bảo An ninh
Hệ thống đạt chuẩn an toàn bảo mật cho giai đoạn phát hành Release Candidate (RC1), đáp ứng đầy đủ tiêu chí vận hành trên môi trường đám mây và máy chủ phân tán.
