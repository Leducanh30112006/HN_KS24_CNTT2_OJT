# HƯỚNG DẪN TRIỂN KHAI HỆ THỐNG LÊN VPS (DEPLOYMENT GUIDE)

> **Dự án:** QuickShop Multi-Vendor Marketplace  
> **Người thực hiện:** Lê Đức Anh - DevOps & Security Lead  
> **Ngày cập nhật:** 13/11/2026  
> **Máy chủ triển khai:** VPS Ubuntu Linux (`221.121.5.123`)  

---

## 1. Thông số Kỹ thuật Máy chủ VPS Production

| Thông số | Giá trị cấu hình | Ghi chú |
| :--- | :--- | :--- |
| **Địa chỉ IP máy chủ (Host)** | `221.121.5.123` | IP tĩnh công khai của VPS |
| **Cổng SSH (SSH Port)** | `22` | Cổng kết nối quản trị từ xa |
| **Tài khoản quản trị (User)** | `root` | Quyền Superuser hệ điều hành |
| **Hệ điều hành khuyến nghị** | Ubuntu 22.04 LTS / 24.04 LTS | Kiến trúc x86_64 |
| **Thư mục triển khai dự án** | `/opt/quickshop` | Nơi chứa mã nguồn và cấu hình Docker |

---

## 2. Cấu trúc Triển khai Phân tán trên VPS

Toàn bộ hệ thống QuickShop trên VPS được điều phối qua **Docker Compose Production (`docker-compose.prod.yml`)** gồm 14 containers phân tầng:

```
[Internet Traffic]
       │
       ▼
[Nginx Reverse Proxy: Port 80 & 443 SSL] ── (Terminates SSL, HTTP/2, Static Cache)
       │
       ├───────────────────────────────┬───────────────────────────────┐
       ▼                               ▼                               ▼
[React 18 Frontend]           [Spring Cloud Gateway: 8080]    [MinIO Console: 9001]
(SPA Web Nginx: Port 80)      (JWT Auth, Rate Limit, CORS)
                                       │
        ┌──────────────┬───────────────┼───────────────┬──────────────┬──────────────┐
        ▼              ▼               ▼               ▼              ▼              ▼
 [Identity: 8081] [Catalog: 8082] [Order: 8083] [Payment: 8084] [Shipping: 8085] [Notification AI: 8086]
        │              │               │               │              │              │
        └──────────────┴───────────────┴───────────────┴──────────────┴──────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    ▼                  ▼                  ▼
             [MySQL 8.0: 3306]   [Redis 7: 6379]  [RabbitMQ 3.13: 5672/15672]
```

---

## 3. Cấu hình GitHub Secrets cho CI/CD Pipeline

Để GitHub Actions tự động build kiểm thử và deploy mỗi khi merge code vào nhánh `main`, cần cấu hình các Secrets sau tại **GitHub Repository > Settings > Secrets and variables > Actions**:

| Secret Key | Giá trị mẫu / Hướng dẫn | Bắt buộc |
| :--- | :--- | :--- |
| `VPS_HOST` | `221.121.5.123` | Có (Mặc định đã có fallback trong workflow) |
| `VPS_PORT` | `22` | Có (Mặc định 22) |
| `VPS_USERNAME` | `root` | Có (Mặc định root) |
| `VPS_PASSWORD` | Mật khẩu truy cập root của VPS | Có (Nếu dùng xác thực mật khẩu) |
| `VPS_SSH_KEY` | Private SSH Key `id_rsa` hoặc `ed25519` | Tùy chọn (Nếu dùng xác thực SSH Key) |
| `DOCKER_USERNAME` | Tài khoản Docker Hub (ví dụ: `quickshop`) | Tùy chọn nếu push public images |
| `DOCKER_PASSWORD` | Docker Hub Access Token | Tùy chọn nếu push public images |

> [!NOTE]
> CI/CD Pipeline của QuickShop đã được cấu hình **hỗ trợ song song cả SSH Key lẫn SSH Password (thông qua `sshpass`)**. Nếu bạn chưa cấu hình SSH Key trên VPS, chỉ cần thêm `VPS_PASSWORD` vào GitHub Secrets là pipeline sẽ tự động upload JAR và kích hoạt deploy.

---

## 4. Hướng dẫn Khởi tạo Môi trường VPS Lần đầu (1-Command Setup)

Khi VPS mới được kích hoạt, bạn có thể thiết lập toàn bộ môi trường và khởi chạy QuickShop chỉ bằng **1 lệnh duy nhất**:

### Cách 1: Chạy trực tiếp từ script tự động (Khuyến nghị)
Đăng nhập SSH vào VPS:
```bash
ssh root@221.121.5.123
```
Chạy lệnh khởi tạo:
```bash
curl -fsSL https://raw.githubusercontent.com/Leducanh30112006/HN_KS24_CNTT2_OJT/dev/ducanh/deployments/scripts/setup-vps.sh | bash
```

### Cách 2: Khởi tạo thủ công từng bước
1. **Cập nhật hệ thống & cài đặt Docker, Git, OpenSSL:**
   ```bash
   apt-get update && apt-get install -y git curl docker.io docker-compose-plugin openssl ufw
   systemctl enable --now docker
   ```
2. **Mở tường lửa UFW:**
   ```bash
   ufw allow 22/tcp
   ufw allow 80/tcp
   ufw allow 443/tcp
   ufw --force enable
   ```
3. **Tải mã nguồn về thư mục `/opt/quickshop`:**
   ```bash
   mkdir -p /opt/quickshop && cd /opt/quickshop
   git clone https://github.com/Leducanh30112006/HN_KS24_CNTT2_OJT.git .
   ```
4. **Sinh chứng chỉ SSL tự ký cho IP `221.121.5.123`:**
   ```bash
   bash ./deployments/nginx/generate-ssl.sh
   ```
5. **Cấu hình biến môi trường & khởi động:**
   ```bash
   cp .env.example .env
   docker compose -f docker-compose.prod.yml up -d --build
   ```

---

## 5. Quy trình Tự động hóa CI/CD (GitHub Actions Workflow)

Tệp workflow `.github/workflows/deploy.yml` thực thi theo quy trình 4 giai đoạn chuẩn DevOps:

1. **Detect Changes (Phát hiện thay đổi):** Sử dụng `paths-filter` để xác định chính xác service nào có sự thay đổi code nhằm tối ưu thời gian build.
2. **Quality Gate (Kiểm thử tự động):**
   - Chạy toàn bộ Unit & Integration Tests của 7 microservices backend (`./gradlew test`).
   - Kiểm tra compile & build giao diện React Frontend (`npm run build`).
   - Ngăn chặn triệt để code lỗi merge vào production.
3. **Build & Package:**
   - Biên dịch file JAR độc lập cho từng microservice bằng Gradle Temurin 17.
   - Đóng gói container JRE tối ưu dung lượng RAM (SerialGC, ContainerSupport).
   - Đóng gói Frontend Nginx SPA container.
4. **Deploy VPS (Rolling Update):**
   - Tải file JAR đã build lên máy chủ `/opt/quickshop/<service>/build/libs/`.
   - Kết nối SSH tới `221.121.5.123` qua `appleboy/ssh-action`.
   - Cập nhật Git code mới nhất trên VPS (`git fetch origin main && git reset --hard origin/main`).
   - Khởi động lại service bị thay đổi với `docker compose up -d --no-deps --build <service>`.
   - Reload Nginx cấu hình dynamic DNS routing mà không gây gián đoạn người dùng (Zero-downtime).

---

## 6. Kiểm tra & Giám sát Sau Triển khai

Truy cập các địa chỉ sau trên máy tính của bạn:
- **Trang chủ QuickShop:** `http://221.121.5.123` hoặc `https://221.121.5.123`
- **API Gateway Health:** `http://221.121.5.123/api/actuator/health`
- **RabbitMQ Dashboard:** `http://221.121.5.123:15672` (User: `quickshop` / Pass: `quickshop123`)
- **MinIO Storage Console:** `http://221.121.5.123:9001` (User: `minioadmin` / Pass: `minioadmin`)
- **Prometheus Metrics:** `http://221.121.5.123:9090`
- **Grafana Dashboard:** `http://221.121.5.123:3000` (User: `admin` / Pass: `quickshop123`)
- **Zipkin Tracing:** `http://221.121.5.123:9411`
