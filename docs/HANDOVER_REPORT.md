# BIÊN BẢN BÀN GIAO DỰ ÁN & PHÂN CÔNG BẢO TRÌ (PROJECT HANDOVER REPORT)

**Tên dự án:** Sàn Thương Mại Điện Tử Đa Người Bán QuickShop (Marketplace Edition)  
**Thời gian hoàn thành & bàn giao:** 20/11/2026  
**Địa điểm:** Trường Đại học Công nghệ Bưu chính Viễn thông (PTIT)  
**Quy mô triển khai:** 4 Sprints (26/10/2026 – 20/11/2026), 20 ngày làm việc (Nghỉ Thứ 7 & Chủ Nhật)

---

## 1. Đại diện các bên tham gia bàn giao

### Nhóm phát triển (Project Team 03):
1. **Lê Đức Anh** - Trưởng nhóm, DevOps & Security Lead, System Architect
2. **Nguyễn Văn Kiên** - Backend Lead, Saga Orchestration, Payment & Shipping Architect
3. **Triệu Quốc Bình** - Frontend Lead, UI/UX Specialist & Integration Lead

---

## 2. Danh mục Hạng mục Bàn giao (Deliverables)

1. **Toàn bộ Mã nguồn Dự án (Source Code):**
   - Kho mã nguồn Git hoàn chỉnh với 389 commits chi tiết từng tính năng bằng tiếng Việt.
   - Các nhánh tính năng đã hoàn thiện và tích hợp vào `main`: `dev/kien`, `quocbinh`, `dev/ducanh`.
2. **Hệ thống Microservices Backend (7 Core Services):**
   - **API Gateway (8080):** Spring Cloud Gateway, JWT Dual Auth filter, Redis token blacklist, rate limit, CORS, Correlation ID.
   - **Identity Service (8081):** Quản lý tài khoản, Google OAuth 2.0, OTP email Redis TTL 5 phút, KYC Shop, RBAC (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`, `ROLE_SHIPPER`).
   - **Catalog Service (8082):** Danh mục đa cấp, sản phẩm đa biến thể SKU, kiểm soát tồn kho lạc quan (@Version), cache Redis, upload ảnh MinIO và CKEditor 5.
   - **Order Service (8083):** Giỏ hàng Redis phân tán đa shop, Checkout tách Sub-Orders, Saga Orchestrator qua RabbitMQ, Dead Letter Queue (`order.dlq`), tự động hủy đơn sau TTL.
   - **Payment Service (8084):** Cổng thanh toán VNPay Sandbox, PayOS chuẩn VietQR EMVCo dynamic QR, Sổ cái kép (Double-entry Ledger), tài khoản ký quỹ trung gian Escrow, tự động giải phóng tiền cho người bán sau 72h.
   - **Shipping Service (8085):** Bộ tính cước GHN/GHTK linh hoạt, sinh mã vận đơn, xử lý Webhook Idempotency theo `event_id`, simulator giao vận và API dành riêng cho bưu tá.
   - **Notification AI Service (8086):** Gửi email thông báo hóa đơn tự động qua RabbitMQ, Trợ lý AI Google Gemini tư vấn mua sắm, tự động sinh mô tả SEO cho Seller.
   - **Thư viện chia sẻ common-library:** ApiResponse chuẩn, mã lỗi nghiệp vụ, Exception Handler và bộ Event Contracts cho Saga.
3. **Ứng dụng Frontend SPA Hoàn chỉnh (4 Phân hệ):**
   - **Cổng Người Mua (Buyer Portal):** Trang chủ Flagship Bento Grid, tìm kiếm & bộ lọc modal, giỏ hàng phân tách theo shop phong cách Shopee, mã giảm giá kép (Sàn + Shop), bản đồ chọn địa chỉ Leaflet OpenStreetMap với GPS Nominatim, lịch sử đơn hàng dạng thẻ phân tách theo shop.
   - **Kênh Người Bán (Seller Center):** Đăng ký KYC gian hàng, quản lý danh mục/SKU, soạn thảo mô tả phong phú CKEditor 5, AI sinh mô tả SEO, quản lý đơn hàng con, bảng thống kê doanh thu theo từng sản phẩm SKU, rút tiền ví.
   - **Cổng Quản Trị Hệ Thống (Admin Portal):** Phê duyệt KYC shop, kiểm duyệt sản phẩm của người bán, quản lý voucher sàn, quản lý người dùng, dashboard tài chính GMV/Commission 5%/Escrow, duyệt rút tiền, hệ thống quản lý Flash Sale đa chế độ (Thủ công & AI Mode có Auto-Approve).
   - **Kênh Nhân viên Giao Hàng (Shipper Portal):** Đăng ký/đăng nhập shipper, danh sách đơn cần lấy (`READY_TO_PICK`), danh sách đang giao (`DELIVERING`), nút bấm cập nhật lộ trình và xác nhận đã giao thành công.
   - **Trang Giới thiệu 3D Scrollytelling (IntroPage):** Trình diễn 6 phân hệ công nghệ cao bằng WebGL/Canvas, chuyển động mượt mà với GSAP và Lenis Scroll.
   - **Hệ thống Tin nhắn Thời gian thực (Full-page Messenger):** Trò chuyện trực tiếp Người mua - Người bán phong cách Shopee/Facebook Messenger, tích hợp thẻ sản phẩm thông minh.
4. **Hạ tầng Triển khai, Bảo mật & Giám sát:**
   - Cấu hình Docker Compose môi trường production (`docker-compose.prod.yml`) tối ưu RAM cho VPS.
   - Cấu hình Nginx Edge Reverse Proxy (`deployments/nginx/nginx.conf`) tích hợp SSL, CSP, OSM Tile Proxy và Nominatim Geocoding Proxy.
   - Giám sát hiệu năng hệ thống với Prometheus TSDB & Grafana Dashboards.
   - Truy vết độ trễ phân tán (Distributed Tracing) OpenZipkin.
   - Tự động hóa CI/CD qua GitHub Actions (`.github/workflows/deploy.yml`).
   - Gia cố an ninh 18/18 tiêu chí QA (JWT Base64URL, SQLi fast-reject, IDOR protection, Nginx Security Headers).
5. **Bộ Hồ sơ Báo cáo & Tài liệu Kỹ thuật:**
   - `README.md` & `.env.example`
   - `docs/PROJECT_PLAN_AND_WORK_BREAKDOWN_4_SPRINTS.md` (Báo cáo tổng kết dự án & Phân công công việc 4 Sprint chuẩn Agile/Scrum)
   - `QuickShop_Trello_Import_Updated.csv` (File phân công công việc 60 task chi tiết cho Trello)
   - `docs/USER_GUIDE.md` (Hướng dẫn sử dụng chi tiết 4 phân hệ)
   - `docs/QA_BACKEND_TEST_REPORT.md` & `docs/QA_FRONTEND_TEST_REPORT.md`
   - `docs/BACKEND_REGRESSION_REPORT.md` & `docs/KNOWN_ISSUES.md`
   - `docs/SECURITY_CHECKLIST.md` & `docs/RELEASE_CHECKLIST.md`

---

## 3. Phân công Trách nhiệm Bảo trì & Hỗ trợ Vận hành

| Thành viên | Phân hệ chịu trách nhiệm bảo hành | Nội dung công việc bảo trì |
| :--- | :--- | :--- |
| **Lê Đức Anh** | Hạ tầng, Bảo mật, CI/CD, Gateway, Identity & AI | Giám sát uptime container VPS, gia hạn chứng chỉ SSL, kiểm soát hạn ngạch Gemini API, rà soát log bảo mật và duy trì pipeline deploy |
| **Nguyễn Văn Kiên** | Nghiệp vụ Đơn hàng, Saga, PayOS/VNPay, Shipping & Sổ cái | Theo dõi luồng bồi hoàn Saga, đối soát số dư ví/ký quỹ Escrow, duy trì webhook PayOS/VNPay và tối ưu hiệu năng cơ sở dữ liệu MySQL |
| **Triệu Quốc Bình** | Giao diện Người dùng, Trải nghiệm 4 phân hệ & Messenger | Thu thập phản hồi người dùng, tinh chỉnh tương tác giao diện trên các dòng thiết bị mới, tối ưu hóa tốc độ tải trang và nâng cấp tiện ích chat |

---

## 4. Xác nhận & Ký kết Bàn giao

Toàn bộ các mục tiêu trong kế hoạch công việc dự án QuickShop từ ngày **26/10/2026 đến ngày 20/11/2026** (trải qua đúng **4 Sprint**, 20 ngày làm việc chính thức, hoàn toàn **không làm việc vào Thứ 7 và Chủ Nhật**) của cả nhóm đã được thực hiện đầy đủ, vượt tiến độ cam kết ban đầu, bổ sung thành công nhiều tính năng mở rộng cao cấp và đạt **100% tiêu chí nghiệm thu**.

| Đại diện Kỹ thuật Backend | Đại diện Thiết kế & Frontend | Trưởng nhóm & Điều phối DevOps |
| :---: | :---: | :---: |
| *(Đã ký)* | *(Đã ký)* | *(Đã ký)* |
| **Nguyễn Văn Kiên** | **Triệu Quốc Bình** | **Lê Đức Anh** |
