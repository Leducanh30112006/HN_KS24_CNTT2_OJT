# Hướng Dẫn & Báo Cáo Kiểm Thử Tải QuickShop (k6 Load Testing Suite)

Bộ kịch bản kiểm thử tải đạt chuẩn công nghiệp cho nền tảng thương mại điện tử **QuickShop Marketplace (SRS v2.2.0)**, được thiết kế để đo lường độ chịu tải, độ trễ và phân tích điểm nghẽn (bottleneck) với mục tiêu **1.000 requests/second (RPS)**.

---

## 1. Cấu Trúc Thư Mục Kiểm Thử Tải

```text
load-tests/
├── bin/
│   └── k6.exe                     # Công cụ đo tải Grafana k6 standalone (v0.56.0 x64)
├── data/
│   ├── test-data.json             # Dữ liệu kiểm thử: Users, SKU Variants, Vouchers, Addresses
│   └── sample-checkout.json       # Payload mẫu kiểm tra đơn hàng qua curl/HTTP
├── scripts/
│   ├── smoke-test.js              # Kịch bản kiểm tra chức năng ở mức tải thấp (5 VUs)
│   ├── read-workload.js           # Kịch bản chuyên biệt: Tải Đọc dữ liệu (800 RPS)
│   ├── write-workload.js          # Kịch bản chuyên biệt: Tải Ghi dữ liệu Cart & Checkout (200 RPS)
│   └── quickshop-load-test.js     # KỊCH BẢN CHÍNH: Tổng hợp 1.000 RPS (80% Read / 20% Write)
├── reports/
│   ├── summary.json               # Số liệu thô JSON chi tiết toàn bộ metrics của k6
│   ├── summary.md                 # Báo cáo tổng hợp số liệu thực tế định dạng Markdown
│   └── load-test-report.html      # Báo cáo Dashboard trực quan với thẻ chỉ số & bảng SLA
└── README.md                      # Tài liệu hướng dẫn, điều chỉnh tải & phân tích chuyên sâu
```

---

## 2. Thiết Kế Workload Phân Tách (Read vs Write)

Mô hình lưu lượng thương mại điện tử thực tế tuân theo tỷ lệ **80% Đọc (Read-heavy) / 20% Ghi (Write-heavy)**:

### 2.1. Workload Đọc (80% Tổng Lưu Lượng - Mục Tiêu 800 RPS)
- **`GET /api/cart` (50% Read)**: Truy vấn giỏ hàng phân tán của người dùng từ Redis Hash Cache.
- **`GET /api/vouchers/{code}` (30% Read)**: Tra cứu thông tin chi tiết voucher toàn sàn (`QUICKSHOP10`) và voucher shop (`SHOP1_SALE10`).
- **`POST /api/vouchers/preview` (20% Read)**: Tính toán xem trước giá trị giảm giá voucher kép dựa trên subtotal đơn hàng.

### 2.2. Workload Ghi (20% Tổng Lưu Lượng - Mục Tiêu 200 RPS)
- **`POST /api/cart/items` (50% Write)**: Thêm mặt hàng SKU vào giỏ hàng Redis.
- **`POST /api/cart/merge` (25% Write)**: Hợp nhất danh sách mặt hàng từ Guest Cart vào User Cart kèm Idempotency Key.
- **`POST /api/orders/checkout` (25% Write)**: Giao dịch phân tán Checkout đơn hàng đa shop, áp dụng voucher kép, kích hoạt Distributed Lock chống trùng lặp, khởi tạo Master Order & Sub-Orders và phát lệnh giữ kho Saga.

---

## 3. Chuẩn Bị Dữ Liệu Kiểm Thử (Tránh Lỗi Hết Hàng Ảo)

Để tránh lỗi giả tạo do thiếu tồn kho hoặc hết lượt dùng voucher khi chạy hàng chục nghìn lượt request:
1. **Biến thể SKU (`variants`)**: Cấu hình 6 SKU thuộc 3 shop khác nhau, mỗi SKU có tồn kho khả dụng lên tới **500.000 đơn vị**.
2. **Voucher Sàn & Shop (`vouchers`)**:
   - `QUICKSHOP10` (Giảm 10%, max 50k, min 100k, `usage_limit: 1.000.000`).
   - `SHOP1_SALE10` (Giảm 10% cho shop 1, min 50k, `usage_limit: 1.000.000`).
   - Thời hạn hiệu lực: 2 năm, trạng thái `ACTIVE`.
3. **Người dùng thử nghiệm (`users`)**: 10 tài khoản Buyer định danh (`1001` - `1010`) luân phiên tạo request.
4. **Idempotency Key độc nhất**: Mỗi lượt write/checkout sinh mã UUID/timestamp riêng biệt (`chk-timestamp-random`), đảm bảo đơn hàng được xử lý thực tế và không bị chặn bởi bộ lọc chống trùng.

---

## 4. Cơ Chế An Toàn: Bảo Vệ Môi Trường Production

Tuân thủ nghiêm ngặt **Yêu cầu số 9** ("Không chạy trên production nếu chưa được cho phép"), tất cả kịch bản k6 đều tích hợp cơ chế **Production Guard**:
- Tự động kiểm tra chuỗi `BASE_URL`.
- Nếu phát hiện domain production (`quickshop.com`, `api.quickshop.vn`, `production`), script sẽ **lập tức ném lỗi ngoại lệ (Abort)** và dừng k6 ngay lập tức.
- Chỉ khi kỹ sư kiểm thử truyền cờ môi trường tường minh `-e ALLOW_PROD_TEST=true` thì bài test mới được cấp quyền thực thi.

```javascript
const isProdUrl = BASE_URL.toLowerCase().includes('production') ||
    BASE_URL.toLowerCase().includes('quickshop.com') ||
    BASE_URL.toLowerCase().includes('api.quickshop.vn');

if (isProdUrl && __ENV.ALLOW_PROD_TEST !== 'true') {
  throw new Error('[CRITICAL SECURITY ALERT] BASE_URL là môi trường PRODUCTION! Kịch bản bị từ chối.');
}
```

---

## 5. Hướng Dẫn Thực Thi Từng Bước (Progressive Load Testing)

### Bước 1: Khởi động Order Service
Trong môi trường phát triển local, có thể khởi động server HTTP thật trên port `8083` phục vụ k6:
```powershell
.\gradlew.bat :order-service:test --tests "com.quickshop.order.load.OrderServiceLoadServerRunner"
```

### Bước 2: Chạy Smoke Test (Tải thấp - 5 VUs / 10s)
Xác minh tính đúng đắn của toàn bộ các API Cart, Voucher và Checkout trước khi tăng tải:
```powershell
& ".\load-tests\bin\k6.exe" run .\load-tests\scripts\smoke-test.js
```

### Bước 3: Tăng tải dần (Stage 1: 250 RPS -> Stage 2: 500 RPS)
Chạy thử mức tải thấp và trung bình để phát hiện sớm các hiện tượng nghẽn tài nguyên:
```powershell
# Chạy mức tải 250 RPS (200 Read / 50 Write)
& ".\load-tests\bin\k6.exe" run -e TARGET_RPS=250 -e READ_RPS=200 -e WRITE_RPS=50 .\load-tests\scripts\quickshop-load-test.js

# Chạy mức tải 500 RPS (400 Read / 100 Write)
& ".\load-tests\bin\k6.exe" run -e TARGET_RPS=500 -e READ_RPS=400 -e WRITE_RPS=100 .\load-tests\scripts\quickshop-load-test.js
```

### Bước 4: Chạy Kịch Bản Mục Tiêu 1.000 RPS (Peak Stress Test)
Chạy bài test tải composite mục tiêu cao nhất của hệ thống:
```powershell
& ".\load-tests\bin\k6.exe" run .\load-tests\scripts\quickshop-load-test.js
```

### Bước 5: Chạy Phân Tách Kịch Bản Chuyên Biệt
Để cô lập và đánh giá riêng phân hệ Đọc hoặc phân hệ Ghi:
```powershell
# Chỉ đo tải Đọc (800 RPS)
& ".\load-tests\bin\k6.exe" run .\load-tests\scripts\read-workload.js

# Chỉ đo tải Ghi (200 RPS)
& ".\load-tests\bin\k6.exe" run .\load-tests\scripts\write-workload.js
```

---

## 6. Tham Số Điều Chỉnh Tải (Environment Variables)

| Biến môi trường | Ý nghĩa | Mặc định | Ví dụ tùy chỉnh |
| :--- | :--- | :--- | :--- |
| `BASE_URL` | Địa chỉ Gateway/Service mục tiêu | `http://localhost:8083` | `-e BASE_URL=http://staging.quickshop.internal` |
| `TARGET_RPS` | Tổng số RPS mục tiêu | `1000` | `-e TARGET_RPS=1200` |
| `READ_RPS` | Số RPS cho luồng Đọc dữ liệu | `800` (80%) | `-e READ_RPS=960` |
| `WRITE_RPS` | Số RPS cho luồng Ghi dữ liệu | `200` (20%) | `-e WRITE_RPS=240` |
| `ALLOW_PROD_TEST`| Cờ cấp phép chạy trên production | `false` | `-e ALLOW_PROD_TEST=true` |

---

## 7. Kết Quả Đo Lường Thực Tế (Empirical Results)

Số liệu thực nghiệm thu thập được từ bài kiểm tra mục tiêu **1.000 RPS** chạy trên hệ thống:

### 7.1. Bảng Số Liệu Tổng Hợp Thực Tế
| Chỉ số (Metric) | Kết quả đo thực tế | Ngưỡng cam kết (SLA) | Đánh giá |
| :--- | :--- | :--- | :--- |
| **Tổng số Requests xử lý** | **46.674 requests** | N/A | **Hoàn thành 100%** |
| **RPS Duy trì tại Peak Stage** | **1.000,00 req/s** | 1.000 req/s | **ĐẠT MỤC TIÊU (800 Read + 200 Write)** |
| **RPS Trung bình (gồm Warmup)** | **666,81 req/s** | N/A | Toàn bộ 70 giây kiểm thử |
| **Tỷ lệ Lỗi (Error Rate)** | **0,00% (0 lỗi / 46.674 reqs)** | < 2,00% | **VƯỢT CHỈ TIÊU (PASS)** |
| **Độ trễ trung vị p50** | **< 1,00 ms** | < 150 ms | **CỰC KỲ TỐT (PASS)** |
| **Độ trễ phân vị p95** | **1,70 ms** | < 500 ms | **VƯỢT CHỈ TIÊU (PASS)** |
| **Độ trễ tối đa (Max Latency)** | **42,18 ms** | < 2.000 ms | **ỔN ĐỊNH** |

### 7.2. Chi Tiết Từng Phân Hệ Nghiệp Vụ
| Phân hệ / API | Số lượng giao dịch hoàn tất | Độ trễ p95 | Tỷ lệ thành công |
| :--- | :--- | :--- | :--- |
| **Đọc dữ liệu (Cart, Voucher)** | **37.339 requests** | **0,52 ms** | **100,00%** |
| **Thêm & Hợp nhất giỏ hàng** | **6.593 actions** | **11,12 ms** | **100,00%** |
| **Giao dịch Checkout (Saga Tx)**| **2.731 Master Orders** | **12,18 ms** | **100,00%** |

---

## 8. Phân Tích Điểm Nghẽn (Bottleneck Analysis) & Đề Xuất Cải Thiện

Khi triển khai trên môi trường phân tán đa máy chủ (Multi-node Production Cluster), để duy trì bền vững 1.000 RPS thực tế cho toàn bộ microservices, các điểm nghẽn kiến trúc sau cần được chú trọng xử lý:

### 8.1. Embedded Tomcat Thread Pool Contention
- **Hiện tượng**: Mặc định Spring Boot Tomcat cấu hình `server.tomcat.threads.max = 200`. Khi có hơn 200 request đồng thời cần xử lý I/O chậm, hàng đợi request queue sẽ bị đầy, dẫn đến hiện tượng connection timeout hoặc độ trễ p99 tăng vọt.
- **Giải pháp**:
  - Chuyển sang sử dụng **Java 21 Virtual Threads (Project Loom)** bằng cách bật thuộc tính `spring.threads.virtual.enabled: true`. Virtual threads cho phép xử lý hàng chục nghìn kết nối đồng thời với overhead bộ nhớ cực thấp.
  - Điều chỉnh `server.tomcat.threads.max: 500` và `server.tomcat.accept-count: 2000` nếu chạy trên platform thread truyền thống.

### 8.2. HikariCP Database Connection Pool Bottleneck
- **Hiện tượng**: Mặc định HikariCP cấu hình `maximum-pool-size = 10`. Tại mức tải 200 RPS Ghi, 10 connection JDBC sẽ trở thành nút thắt cổ chai, khiến các luồng checkout phải xếp hàng chờ giải phóng connection (`ConnectionTimeoutException`).
- **Giải pháp**:
  - Nâng `spring.datasource.hikari.maximum-pool-size: 50` đến `100` cho mỗi instance order-service.
  - Triển khai mô hình **MySQL Read-Write Splitting (CQRS)**: Các truy vấn voucher/đơn hàng đọc từ MySQL Read Replica, chỉ định tuyến INSERT/UPDATE vào Primary MySQL.

### 8.3. Cạnh Tranh Khóa Phân Tán (Redis Lock Contention)
- **Hiện tượng**: Trong kịch bản checkout, `RedisDistributedLock` được lấy theo `userId` (`lock:checkout:{userId}`). Nếu cùng một tài khoản thực hiện hàng loạt checkout cùng lúc, các giao dịch sau sẽ bị lock contention và phải retry hoặc trả về lỗi `CartLockException`.
- **Giải pháp**:
  - Tối ưu hóa thời gian giữ lock: Chỉ giữ lock trong giai đoạn snapshot và validate dữ liệu; giải phóng lock ngay sau khi Master Order được lưu và trước khi thực hiện các tác vụ phát message RabbitMQ không đồng bộ.
  - Áp dụng cơ chế **Token Bucket Rate Limiting** tại API Gateway (Spring Cloud Gateway / Nginx) theo User IP/ID để ngăn chặn spam checkout.

### 8.4. Hàng Đợi Message Buffer Cho Checkout (Asynchronous Checkout)
- **Hiện tượng**: Khi lượng người mua tăng đột biến (Flash Sale / Campain Sale > 1.000 RPS), việc tạo transaction ghi trực tiếp vào RDBMS đồng bộ sẽ làm cạn kiệt tài nguyên CSDL.
- **Giải pháp**:
  - Chuyển đổi API Checkout sang mô hình **Async Order Ingestion**: Request checkout sau khi validate sơ bộ và giữ kho nhanh trên Redis sẽ được đẩy vào message queue (Kafka / RabbitMQ Quorum Queue).
  - Trả về mã HTTP `202 Accepted` kèm `orderTrackingTicket`. Một nhóm Worker service phía sau sẽ pull message và ghi nhận đơn hàng vào CSDL theo tốc độ xử lý tối ưu, giúp triệt tiêu hoàn toàn rủi ro sập hệ thống khi quá tải.
