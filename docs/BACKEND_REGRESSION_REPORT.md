# BÁO CÁO HỒI QUY TOÀN DIỆN HỆ THỐNG BACKEND (BACKEND REGRESSION REPORT)

**Người thực hiện:** Nguyễn Văn Kiên (Backend Lead & Saga Architect)  
**Ngày thực hiện:** **20/11/2026** (Nghiệm thu Sprint 4)  
**Phiên bản phát hành:** QuickShop Release 1.0 (Production Final)  

---

## 1. Phạm vi hồi quy (Regression Scope)

Kiểm tra toàn diện tính ổn định và tương tác giữa 7 Microservices backend sau khi bổ sung các tính năng nâng cao trong Sprint 4 (PayOS VietQR, Google OAuth, Leaflet Map Proxy, Shipper API, Flash Sale Manager):
1. **API Gateway (Port 8080):** Spring Cloud Gateway, JWT filter Dual Auth, Nominatim/OSM Proxy, Rate Limiting, Correlation ID.
2. **Identity Service (Port 8081):** Auth, Google OAuth 2.0, OTP email Redis TTL 5 phút, KYC Shop, RBAC mở rộng (`BUYER`, `SELLER`, `ADMIN`, `SHIPPER`).
3. **Catalog Service (Port 8082):** Phân cấp danh mục, biến thể SKU, khóa lạc quan (@Version), Redis Cache, upload ảnh MinIO và CKEditor 5.
4. **Order Service (Port 8083):** Giỏ hàng Redis phân tán đa shop, Checkout tách Sub-Orders, Saga Orchestrator, RabbitMQ DLQ (`order.dlq`), tự động hủy đơn sau TTL.
5. **Payment Service (Port 8084):** Cổng thanh toán kép VNPay Sandbox & PayOS VietQR EMVCo, Sổ cái kế toán kép (Ledger), Ký quỹ Escrow và tự động giải phóng tiền ví sau 72h.
6. **Shipping Service (Port 8085):** GHN/GHTK Calculator, sinh mã vận đơn, xử lý Webhook Idempotency theo `event_id`, Mock Simulator và API dành riêng cho bưu tá giao hàng.
7. **Notification AI Service (Port 8086):** Email hóa đơn tự động qua RabbitMQ, Google Gemini API Chatbot tư vấn, Function Calling và AI SEO Generator.

---

## 2. Kết quả kiểm tra hồi quy từng phân hệ

### 2.1. Phân hệ Định danh & Phân quyền (Identity & Auth)
- Đăng nhập JWT, sinh Access Token & Refresh Token: **Hoạt động chính xác**
- Cơ chế thu hồi token (Logout / Revoke) lưu trên Redis: **Hoạt động ổn định**
- Phân quyền theo vai trò (`BUYER`, `SELLER`, `ADMIN`, `SHIPPER`): **Chặn trái phép đúng HTTP 403 Forbidden**
- Đăng nhập & Đăng ký Google OAuth 2.0: **Đồng bộ tài khoản thành công, tự động trích xuất email chuẩn**
- Xác thực OTP Email qua Redis TTL: **Hết hạn chính xác sau 300s, Rate Limiting 60s chặn spam hiệu quả**

### 2.2. Phân hệ Giỏ hàng, Đơn hàng & Saga Orchestration
- Giỏ hàng lưu trữ trên Redis với hash và TTL: **Tốc độ đọc/ghi < 5ms**
- Saga điều phối đơn hàng đa shop (Multi-vendor Sub-Orders):
  - Luồng thành công (Happy path): Tạo đơn -> Giữ chỗ kho -> Khóa voucher -> Thanh toán -> Xác nhận thành công.
  - Luồng bù trừ (Compensation path): Thử nghiệm mock lỗi thanh toán, Saga tự động rollback hàng tồn kho và hoàn trả lượt dùng voucher trong vòng < 200ms.
- Bộ lập lịch quét đơn hết hạn (Scheduler): **Tự động hủy đơn và hoàn kho sau 15 phút không thanh toán**

### 2.3. Phân hệ Thanh toán, Sổ cái kép & Ký quỹ Escrow
- Tích hợp đa cổng thanh toán:
  - Cổng VNPay Sandbox: **Sinh URL thanh toán HMAC-SHA512 chuẩn, IPN cập nhật đơn tức thì**
  - Cổng PayOS VietQR: **Sinh mã QR EMVCo động chứa đúng số tiền và mã đơn hàng, webhook kích hoạt đơn hàng không độ trễ**
- Sổ cái kế toán kép (Double-entry Ledger): **100% bút toán tuân thủ nguyên tắc: Tổng Nợ (Debit) = Tổng Có (Credit)**
- Tài khoản Ký quỹ (Escrow Holding): **Chuyển đổi trạng thái `HOLDING` -> `RELEASED` / `REFUNDED` an toàn tuyệt đối**
- Quyết toán tự động sau 72h: **Scheduled task giải phóng tiền hàng vào ví người bán chính xác sau 72 giờ kể từ khi giao thành công**

### 2.4. Phân hệ Vận chuyển & Kênh Shipper
- Bảng cước vận chuyển linh hoạt theo cân nặng và khoảng cách: **Chính xác**
- Tạo mã vận đơn chuẩn hóa (`GHN-XXXX`, `GHTK-XXXX`): **Chính xác**
- Xử lý Webhook Idempotent: **Thử nghiệm gửi liên tiếp 5 request cùng `event_id`, chỉ request đầu tiên làm thay đổi dữ liệu, 4 request sau trả về phản hồi idempotent ngay lập tức không gây lỗi dữ liệu**
- Kênh bưu tá (`ROLE_SHIPPER`): **Tiếp nhận đơn `READY_TO_PICK` và cập nhật lộ trình `DELIVERING` -> `DELIVERED` trơn tru**

### 2.5. Phân hệ Catalog, Trí tuệ nhân tạo & Flash Sale
- Trình soạn thảo mô tả phong phú CKEditor 5: **Tải ảnh nội tuyến lên MinIO qua kênh HTTPS an toàn**
- Google Gemini AI Chatbot: **Tư vấn chuẩn xác theo ngữ cảnh; Function Calling tìm kiếm sản phẩm hoạt động mượt mà; Circuit Breaker fallback khi cạn quota**
- Cổng quản trị Flash Sale: **Hỗ trợ cả 2 chế độ Manual và AI Auto-Approve, đồng hồ đếm ngược đồng bộ toàn hệ thống**

---

## 3. Tổng kết chỉ số chất lượng

- **Tổng số Unit & Integration Tests:** > 140 test cases.
- **Tỉ lệ Pass:** **100%**.
- **Zero Critical / High Bugs:** Không còn bất kỳ lỗi nào gây sai lệch tiền tệ, âm kho hàng hay treo hệ thống.
- **Bảo mật & An toàn thông tin:** Vượt qua toàn bộ 18/18 tiêu chí an ninh QA.
