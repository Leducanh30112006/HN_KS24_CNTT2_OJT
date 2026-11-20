# BÁO CÁO DỰ ÁN & BẢNG PHÂN CÔNG CÔNG VIỆC 4 SPRINT (PROJECT PLAN & WORK BREAKDOWN)

> **Dự án:** Sàn Thương Mại Điện Tử Đa Người Bán QuickShop (Marketplace Edition)  
> **Khách hàng / Đơn vị nghiệm thu:** Ban Đào tạo OJT - Học viện Công nghệ Bưu chính Viễn thông (PTIT)  
> **Thời gian thực hiện:** **26/10/2026 – 20/11/2026** (Đúng **4 Sprint**, hoàn thành ngày **20/11/2026**)  
> **Quy định làm việc:** **Chỉ làm việc từ Thứ Hai đến Thứ Sáu, NGHỈ Thứ Bảy và Chủ Nhật**  
> **Nhóm thực hiện (Team 03 - CNTT2):**
> - **Lê Đức Anh** - Trưởng nhóm, DevOps & Security Lead, System Architect
> - **Nguyễn Văn Kiên** - Backend Lead, Saga Orchestration, Payment & Shipping Architect
> - **Triệu Quốc Bình** - Frontend Lead, UI/UX Specialist & Integration Lead

---

## MỤC LỤC
1. [Tổng quan dự án & Bối cảnh điều chỉnh](#1-tổng-quan-dự-án--bối-cảnh-điều-chỉnh)
2. [Danh mục các tính năng mở rộng bổ sung mới](#2-danh-mục-các-tính-năng-mở-rộng-bổ-sung-mới)
3. [Kế hoạch phân bổ 4 Sprint (Sprint Schedule)](#3-kế-hoạch-phân-bổ-4-sprint-sprint-schedule)
4. [Bảng phân công công việc chi tiết 60 đầu việc (20 ngày làm việc)](#4-bảng-phân-công-công-việc-chi-tiết-60-đầu-việc-20-ngày-làm-việc)
   - [Sprint 1: Nền tảng Kiến trúc, Database & Khởi tạo Microservices (26/10 - 30/10)](#sprint-1-nền-tảng-kiến-trúc-database--khởi-tạo-microservices-26102026--30102026)
   - [Sprint 2: Định danh bảo mật, Catalog đa biến thể, Giỏ hàng Redis & Phê duyệt Shop (02/11 - 06/11)](#sprint-2-định-danh-bảo-mật-catalog-đa-biến-thể-giỏ-hàng-redis--phê-duyệt-shop-02112026--06112026)
   - [Sprint 3: Saga Orchestration, Thanh toán VNPay, Ký quỹ Escrow & AI Gemini (09/11 - 13/11)](#sprint-3-saga-orchestration-thanh-toán-vnpay-ký-quỹ-escrow--ai-gemini-09112026--13112026)
   - [Sprint 4: Mở rộng tính năng cao cấp, Kênh Shipper, PayOS VietQR, Map Leaflet, Chat, QA & Bàn giao (16/11 - 20/11)](#sprint-4-mở-rộng-tính-năng-cao-cấp-kênh-shipper-payos-vietqr-map-leaflet-chat-qa--bàn-giao-16112026--20112026)
5. [Ma trận phân bổ trách nhiệm (RACI Matrix)](#5-ma-trận-phân-bổ-trách-nhiệm-raci-matrix)
6. [Thống kê khối lượng & Tiêu chí nghiệm thu hệ thống](#6-thống-kê-khối-lượng--tiêu-chí-nghiệm-thu-hệ-thống)

---

## 1. Tổng quan dự án & Bối cảnh điều chỉnh

### 1.1. Bối cảnh ban đầu vs Kế hoạch điều chỉnh mới
- **Kế hoạch ban đầu:** Dự án dự kiến hoàn thành vào ngày 16/11/2026, lịch làm việc bao gồm cả Thứ Bảy và Chủ Nhật, phạm vi chỉ tập trung vào gói MVP cơ bản gồm 3 phân hệ (Buyer, Seller, Admin) với thanh toán VNPay và COD.
- **Kế hoạch chuẩn hóa mới:**
  - **Mốc thời gian kết thúc:** Dự án chính thức hoàn thành và bàn giao toàn diện vào ngày **20/11/2026**.
  - **Cơ cấu chu kỳ:** Phân bổ chính xác thành **4 Sprint**, tương ứng 4 tuần lễ làm việc từ **26/10/2026 đến 20/11/2026**.
  - **Quy tắc làm việc:** **Loại bỏ hoàn toàn các ngày Thứ Bảy và Chủ Nhật**. Mỗi Sprint bao gồm đúng 5 ngày làm việc chính thức (từ Thứ Hai đến Thứ Sáu), tổng cộng **20 ngày làm việc**.
  - **Phạm vi mở rộng:** Bổ sung hàng loạt tính năng cao cấp xuất phát từ yêu cầu thực tế phát triển: Kênh Nhân viên Giao hàng (Shipper Portal), Cổng thanh toán chuyển khoản tự động PayOS VietQR, Bản đồ số tương tác Leaflet OpenStreetMap với GPS Nominatim, Hệ thống Chat thời gian thực Buyer - Seller, Quản trị Flash Sale bằng Trí tuệ nhân tạo (AI Mode), và Trang giới thiệu 3D Scrollytelling.

---

## 2. Danh mục các tính năng mở rộng bổ sung mới

Trong suốt quá trình thực thi, nhóm đã chủ động nghiên cứu và phát triển thêm các phân hệ nghiệp vụ cao cấp, gia tăng giá trị thực tiễn cho nền tảng QuickShop:

| STT | Tính năng mới bổ sung | Mô tả chi tiết & Giá trị nghiệp vụ | Phân hệ phụ trách |
| :---: | :--- | :--- | :--- |
| **1** | **Kênh Nhân viên Giao Hàng (`ROLE_SHIPPER` & Shipper Portal)** | Cung cấp cổng thông tin riêng biệt cho bưu tá giao vận: Đăng ký/đăng nhập shipper, danh sách đơn cần lấy (`READY_TO_PICK`), danh sách đang giao (`DELIVERING`), nút bấm cập nhật tiến trình và xác nhận đã giao thành công. | Identity, Shipping, Frontend |
| **2** | **Cổng thanh toán PayOS chuẩn VietQR EMVCo QR Code** | Tích hợp cổng PayOS sinh mã QR chuyển khoản ngân hàng động (chứa số tiền và mã đơn hàng chính xác); xử lý webhook tự động kích hoạt trạng thái thanh toán và hỗ trợ Admin đối soát thanh toán chuyển khoản thủ công. | Payment Service, Frontend |
| **3** | **Bản đồ số tương tác Leaflet OpenStreetMap & GPS Nominatim** | Tích hợp bản đồ Leaflet OSM trực quan khi chọn địa chỉ giao nhận hàng; công cụ Nominatim Geocoding chuyển đổi địa chỉ thành tọa độ GPS (Kinh độ/Vĩ độ) lưu vào database; cấu hình Nginx Tile Cache Proxy chống Mixed Content. | API Gateway, Nginx, Frontend |
| **4** | **Đăng nhập xã hội Google OAuth 2.0 & Xác thực OTP qua Email** | Hỗ trợ người dùng đăng nhập/đăng ký một chạm qua tài khoản Google; luồng xác thực mã OTP gửi về Email thực qua Redis TTL 5 phút có Rate-Limiting chống spam. | Identity Service, Frontend |
| **5** | **Hệ thống Tin nhắn Thời gian thực (Full-page Messenger)** | Giao diện Chat toàn màn hình phong cách Shopee Chat / Facebook Messenger giữa Người mua và Người bán; tự động nhúng Thẻ sản phẩm thông minh (Intelligent Product Cards) vào khung trò chuyện để tư vấn và chốt đơn ngay lập tức. | Frontend, Identity, Catalog |
| **6** | **Cổng Quản trị Flash Sale đa chế độ (Manual & AI Mode)** | Quản lý chiến dịch Flash Sale với 2 chế độ: Chọn sản phẩm thủ công (Manual) hoặc AI tự động đề xuất và duyệt sản phẩm (AI Mode với Auto-Approve toggle); đồng hồ đếm ngược thời gian thực trên Storefront. | Catalog, Order, Admin Portal |
| **7** | **Trình soạn thảo phong phú CKEditor 5 & Tải ảnh MinIO trực tiếp** | Tích hợp bộ soạn thảo văn bản phong phú CKEditor 5 cho Seller khi viết mô tả sản phẩm; upload ảnh nội tuyến trực tiếp lên kho lưu trữ MinIO qua kênh HTTPS an toàn. | Catalog Service, Seller Center |
| **8** | **Trang Giới thiệu 3D Scrollytelling Flagship (IntroPage)** | Trình diễn tương tác 3D 6 phân hệ của hệ thống bằng WebGL, Three.js và Canvas; kết hợp thư viện cuộn mượt Lenis và chuyển động chuyên nghiệp GSAP theo chuẩn Impeccable Design. | Frontend Core, Storefront |
| **9** | **Bảng Thống kê Phân tích Doanh thu theo từng mã SKU** | Cung cấp bảng dữ liệu tài chính chi tiết từng sản phẩm/biến thể bán ra cho Seller và Admin, giúp theo dõi chính xác sản phẩm sinh lời cao nhất. | Payment, Seller Center, Admin |
| **10**| **Vá toàn diện 18 tiêu chí an ninh & tối ưu tài nguyên VPS** | Khắc phục 18/18 lỗi bảo mật QA (kiểm tra chữ ký Base64URL JWT, SQLi fast-reject, IDOR protection, Nginx Security Headers); tối ưu bộ nhớ JVM chạy ổn định trên VPS 1GB RAM. | API Gateway, Identity, DevOps |

---

## 3. Kế hoạch phân bổ 4 Sprint (Sprint Schedule)

Toàn bộ dự án được thực thi tuần tự và kỷ luật qua **4 Sprint**, mỗi Sprint dài đúng 5 ngày làm việc (Thứ Hai đến Thứ Sáu), không làm việc vào Thứ Bảy và Chủ Nhật:

```
[SPRINT 1: 26/10 -> 30/10] ===> [Nghỉ T7-CN: 31/10 & 01/11]
         |
[SPRINT 2: 02/11 -> 06/11] ===> [Nghỉ T7-CN: 07/11 & 08/11]
         |
[SPRINT 3: 09/11 -> 13/11] ===> [Nghỉ T7-CN: 14/11 & 15/11]
         |
[SPRINT 4: 16/11 -> 20/11] ===> [20/11/2026: NGHIỆM THU & BÀN GIAO TOÀN DIỆN]
```

- **Sprint 1 (26/10/2026 - 30/10/2026):** Khởi tạo dự án, thiết kế kiến trúc phân tán, thiết kế cơ sở dữ liệu và dựng khung hạ tầng Microservices.
- **Sprint 2 (02/11/2026 - 06/11/2026):** Hiện thực dịch vụ Định danh bảo mật, Catalog đa biến thể SKU, Giỏ hàng phân tán Redis và quy trình Đăng ký/Phê duyệt Shop.
- **Sprint 3 (09/11/2026 - 13/11/2026):** Xây dựng Saga Orchestrator, cổng thanh toán VNPay, Sổ cái kế toán kép Escrow, Shipping Service và Trợ lý ảo AI Gemini.
- **Sprint 4 (16/11/2026 - 20/11/2026):** Mở rộng tính năng cao cấp (Kênh Shipper, PayOS VietQR, Bản đồ Leaflet OSM, Chat Messenger, Flash Sale AI), QA Hồi quy, Tối ưu VPS và Bàn giao ngày 20/11/2026.

---

## 4. Bảng phân công công việc chi tiết 60 đầu việc (20 ngày làm việc)

Mỗi ngày làm việc bao gồm 3 đầu việc chuyên trách song song của 3 thành viên: **Lê Đức Anh** (DevOps & Kiến trúc/Bảo mật/AI), **Nguyễn Văn Kiên** (Backend Core/Saga/Thanh toán/Vận chuyển) và **Triệu Quốc Bình** (Frontend/UI-UX/Tích hợp).

### SPRINT 1: NỀN TẢNG KIẾN TRÚC, DATABASE & KHỞI TẠO MICROSERVICES (26/10/2026 – 30/10/2026)

#### Ngày 1: Thứ Hai, 2026-10-26
1. **Lê Đức Anh** - `[Quản lý & Kiến trúc] Kick-off dự án; rà soát mục tiêu MVP, FR-01–FR-11, NFR; lập backlog phạm vi và tiêu chí nghiệm thu`
   - *Phân hệ:* Quản lý & Kiến trúc
   - *Chi tiết:* Khởi động dự án; rà soát tài liệu đặc tả yêu cầu phần mềm (SRS QuickShop Marketplace), xác định phạm vi hệ thống phân tán, kiến trúc microservices và các tiêu chí chất lượng phi chức năng NFR.
   - *Đầu ra:* Biên bản họp kick-off; Backlog phạm vi công việc 4 Sprint; Bộ tiêu chí nghiệm thu Acceptance Criteria.
   - *Phụ thuộc:* Project Charter, SRS QuickShop.
2. **Triệu Quốc Bình** - `[Frontend & Phân tích] Khảo sát yêu cầu người dùng 4 phân hệ; xây dựng sơ đồ luồng điều hướng và wireframe UI/UX`
   - *Phân hệ:* Frontend & Phân tích
   - *Chi tiết:* Khảo sát chi tiết hành trình người dùng 4 phân hệ (Buyer, Seller, Admin, Shipper); thiết lập sơ đồ luồng điều hướng UI, danh mục các màn hình cần xây dựng và cấu trúc trải nghiệm khách hàng.
   - *Đầu ra:* Sơ đồ luồng người dùng (User Journey Map); Danh sách màn hình và wireframe UI 4 phân hệ.
   - *Phụ thuộc:* SRS, Phân quyền vai trò người dùng (Roles).
3. **Nguyễn Văn Kiên** - `[Data & Order] Phân tích nghiệp vụ cốt lõi: Giỏ hàng phân tán, Checkout đa gian hàng, Sổ cái kép và Vận chuyển`
   - *Phân hệ:* Data & Order
   - *Chi tiết:* Rà soát phân tích nghiệp vụ mua sắm đa người bán: Lưu trữ giỏ hàng, quy trình Checkout phân tách Sub-Orders theo Shop, cơ chế thanh toán giữ tiền ký quỹ Escrow và quy tắc vận chuyển.
   - *Đầu ra:* Tài liệu phân tích nghiệp vụ Checkout đa shop; Bảng quy tắc trạng thái đơn hàng và chuyển đổi trạng thái (State Machine).
   - *Phụ thuộc:* SRS QuickShop.

#### Ngày 2: Thứ Ba, 2026-10-27
4. **Lê Đức Anh** - `[Kiến trúc & Hạ tầng] Thiết kế sơ đồ kiến trúc tổng thể 1 API Gateway + 6 Backend Services và phân vùng mạng`
   - *Phân hệ:* Kiến trúc & Hạ tầng
   - *Chi tiết:* Chốt sơ đồ kiến trúc hệ thống gồm Edge Nginx Reverse Proxy, Spring Cloud API Gateway (Port 8080) và 6 Microservices backend; định hình giao tiếp REST đồng bộ và Message Broker bất đồng bộ.
   - *Đầu ra:* Sơ đồ kiến trúc microservices tổng thể v1; Ma trận cổng kết nối (Port Matrix) và tài liệu ranh giới trách nhiệm.
   - *Phụ thuộc:* Kết quả Kick-off và SRS.
5. **Triệu Quốc Bình** - `[Frontend Core] Khởi tạo kiến trúc dự án React 18, Vite, Tailwind CSS và thiết lập Routing cơ bản`
   - *Phân hệ:* Frontend Core
   - *Chi tiết:* Khởi tạo source code frontend với React 18 + Vite + Tailwind CSS; cấu hình alias đường dẫn, quản lý trạng thái tập trung với Zustand (Auth, Cart, UI) và dựng layout khung cho các phân hệ.
   - *Đầu ra:* Mã nguồn khởi tạo Frontend chạy dev local; Cấu trúc thư mục chuẩn và danh sách route / layout.
   - *Phụ thuộc:* Danh sách màn hình UI.
6. **Nguyễn Văn Kiên** - `[Database Design] Thiết kế mô hình dữ liệu quan hệ ERD cho các phân hệ Order, Payment, Shipping và Notification`
   - *Phân hệ:* Database Design
   - *Chi tiết:* Thiết kế lược đồ thực thể quan hệ (ERD) cho từng microservice theo nguyên tắc Database-per-service; xác định các khóa chính, khóa ngoại logic, chỉ mục đánh index và snapshot dữ liệu giá/tên hàng.
   - *Đầu ra:* Sơ đồ ERD nhóm dịch vụ Order, Payment, Shipping, Notification; Tài liệu từ điển dữ liệu (Data Dictionary).
   - *Phụ thuộc:* Bảng nghiệp vụ Checkout & Tài chính.

#### Ngày 3: Thứ Tư, 2026-10-28
7. **Lê Đức Anh** - `[DevOps & Hạ tầng] Thiết lập quy chuẩn Git, Branch Convention và Docker Compose cơ sở cho cơ sở dữ liệu MySQL 8.0`
   - *Phân hệ:* DevOps & Hạ tầng
   - *Chi tiết:* Khởi tạo repository Git, cấu hình branch protection và các nhánh làm việc (`dev/ducanh`, `dev/kien`, `quocbinh`, `main`); viết docker-compose nền khởi chạy MySQL 8.0 phân vùng schema.
   - *Đầu ra:* Git repository chuẩn hóa; Script Docker Compose MySQL chạy ổn định; File mẫu `.env.example` chuẩn.
   - *Phụ thuộc:* Sơ đồ kiến trúc tổng thể.
8. **Triệu Quốc Bình** - `[Catalog & Database] Thiết kế Schema Catalog DB: Category, Product, SKU, Image và Review`
   - *Phân hệ:* Catalog & Database
   - *Chi tiết:* Thiết kế lược đồ dữ liệu dịch vụ Catalog: Bảng Category phân cấp cha-con, Product, ProductSku lưu trữ biến thể, ProductImage và ProductReview; xác định enum trạng thái và ràng buộc toàn vẹn.
   - *Đầu ra:* Bản vẽ ERD Catalog DB; Script DDL khởi tạo bảng Catalog v1.
   - *Phụ thuộc:* SRS Catalog.
9. **Nguyễn Văn Kiên** - `[Order DB & Entities] Hoàn thiện DDL Schema Order DB: MasterOrder, SubOrder, OrderItem và VoucherUsage`
   - *Phân hệ:* Order DB & Entities
   - *Chi tiết:* Xây dựng chi tiết script tạo bảng Order DB: `master_orders`, `sub_orders`, `order_items`, `voucher_usages`; cấu hình enum trạng thái `OrderStatus` và các trường audit log.
   - *Đầu ra:* Script DDL `order_service_init.sql`; Lớp thực thể Entity và Repository JPA Order.
   - *Phụ thuộc:* Sơ đồ ERD Order.

#### Ngày 4: Thứ Năm, 2026-10-29
10. **Lê Đức Anh** - `[Hạ tầng Phân tán] Mở rộng Docker Compose: Tích hợp Redis 7, RabbitMQ 3.13 và MinIO S3 Object Storage`
    - *Phân hệ:* Hạ tầng Phân tán
    - *Chi tiết:* Bổ sung container Redis 7 (lưu giỏ hàng & cache), RabbitMQ 3.13 Management (Message Broker cho Saga) và MinIO (lưu ảnh sản phẩm/KYC cổng 9010/9011) vào Docker Compose; cấu hình network và healthcheck.
    - *Đầu ra:* Cụm hạ tầng Docker Compose hoàn chỉnh chạy ổn định; Dashboard RabbitMQ và MinIO sẵn sàng kết nối.
    - *Phụ thuộc:* Docker Compose MySQL.
11. **Triệu Quốc Bình** - `[Frontend Storefront] Xây dựng Storefront Layout: Header Shopee-style, Drawer giỏ hàng và Footer`
    - *Phân hệ:* Frontend Storefront
    - *Chi tiết:* Phát triển giao diện khung trang chủ mua sắm: Thanh điều hướng Header với ô tìm kiếm nhanh, icon giỏ hàng động hiển thị badge số lượng, menu điều hướng danh mục và footer liên hệ.
    - *Đầu ra:* Giao diện Layout Storefront chuẩn responsive; Component Drawer giỏ hàng trượt mượt mà.
    - *Phụ thuộc:* Thiết lập Frontend Core.
12. **Nguyễn Văn Kiên** - `[Common Library] Xây dựng thư viện dùng chung common-library cho toàn bộ các microservices backend`
    - *Phân hệ:* Common Library
    - *Chi tiết:* Xây dựng module dùng chung `common-library`: Định nghĩa chuẩn `ApiResponse<T>`, bộ mã lỗi nghiệp vụ chuẩn, `GlobalExceptionHandler`, base Entity có auditing và các DTO chuyển giao dữ liệu.
    - *Đầu ra:* Thư viện `common-library` build thành công; Tích hợp dependency vào cấu hình root `build.gradle`.
    - *Phụ thuộc:* DDL và Entity models.

#### Ngày 5: Thứ Sáu, 2026-10-30
13. **Lê Đức Anh** - `[API Gateway] Khởi tạo Spring Cloud Gateway (Port 8080); cấu hình định tuyến cơ bản, CORS và Correlation ID`
    - *Phân hệ:* API Gateway
    - *Chi tiết:* Tạo module API Gateway với Spring Cloud Gateway; cấu hình route ánh xạ tới Identity, Catalog, Order; thiết lập bộ lọc xử lý CORS toàn hệ thống và sinh `X-Correlation-Id` truy vết request.
    - *Đầu ra:* Gateway Service khởi động trên cổng 8080; Chuyển tiếp request tới các service trơn tru kèm log tracing.
    - *Phụ thuộc:* Cụm hạ tầng Docker.
14. **Triệu Quốc Bình** - `[Catalog Service] Khởi tạo Catalog Service (Port 8082); cấu hình kết nối DB, migration và API Category CRUD`
    - *Phân hệ:* Catalog Service
    - *Chi tiết:* Khởi tạo Catalog Service bằng Spring Boot 3.3; cấu hình kết nối MySQL Catalog DB; hiện thực tầng Controller, Service và Repository cho danh mục hàng hóa (Category CRUD) có kiểm tra hợp lệ.
    - *Đầu ra:* Catalog Service chạy trên cổng 8082; Bộ API Category CRUD hoạt động kèm kiểm thử unit test.
    - *Phụ thuộc:* DDL Catalog, common-library.
15. **Nguyễn Văn Kiên** - `[Order Service] Khởi tạo Order Service (Port 8083); thiết lập kết nối MySQL, Redis và contract API giỏ hàng`
    - *Phân hệ:* Order Service
    - *Chi tiết:* Khởi tạo Order Service; cấu hình kết nối cơ sở dữ liệu Order DB và Redis; thiết lập API contract cho giỏ hàng phân tán (Cart DTO) và bộ xử lý ngoại lệ giỏ hàng.
    - *Đầu ra:* Order Service khởi động thành công trên cổng 8083; API contract giỏ hàng chuẩn OpenAPI.
    - *Phụ thuộc:* DDL Order, common-library.

> 🛑 **NGHỈ CUỐI TUẦN:** Thứ Bảy, 31/10/2026 & Chủ Nhật, 01/11/2026 — Không làm việc.

---

### SPRINT 2: ĐỊNH DANH BẢO MẬT, CATALOG ĐA BIẾN THỂ, GIỎ HÀNG REDIS & PHÊ DUYỆT SHOP (02/11/2026 – 06/11/2026)

#### Ngày 6: Thứ Hai, 2026-11-02
16. **Lê Đức Anh** - `[Identity & Auth] Khởi tạo Identity Service (Port 8081); hiện thực API đăng ký tài khoản và mã hóa BCrypt`
    - *Phân hệ:* Identity & Auth
    - *Chi tiết:* Khởi tạo Identity Service; xây dựng thực thể User, Role, ShopProfile; triển khai API đăng ký tài khoản với mã hóa mật khẩu BCrypt, kiểm tra trùng lặp Email và Số điện thoại.
    - *Đầu ra:* Identity Service chạy trên cổng 8081; API đăng ký hoạt động có kiểm tra tính hợp lệ dữ liệu đầu vào.
    - *Phụ thuộc:* API Gateway, common-library.
17. **Triệu Quốc Bình** - `[Catalog Management] Triển khai API quản lý Sản phẩm theo Shop và vòng đời phê duyệt sản phẩm`
    - *Phân hệ:* Catalog Management
    - *Chi tiết:* Phát triển Product REST API: Thêm, sửa, xóa sản phẩm theo Shop; cài đặt máy trạng thái sản phẩm (`DRAFT` -> `PENDING_APPROVAL` -> `APPROVED` / `REJECTED`) và phân quyền thao tác.
    - *Đầu ra:* Bộ API Product CRUD hoàn chỉnh; Kiểm thử đơn vị kiểm tra quyền truy cập theo từng gian hàng.
    - *Phụ thuộc:* Category CRUD.
18. **Nguyễn Văn Kiên** - `[Redis Cart] Triển khai lưu trữ giỏ hàng phân tán trên Redis: Guest Cart và User Cart`
    - *Phân hệ:* Redis Cart
    - *Chi tiết:* Xây dựng CartService trên Redis: Sử dụng Redis Hash lưu trữ mặt hàng trong giỏ; cài đặt cơ chế TTL 7 ngày cho giỏ hàng khách và vô hạn cho người dùng đăng nhập; hỗ trợ thao tác thêm/sửa/xóa SKU.
    - *Đầu ra:* API Giỏ hàng trên Redis với độ trễ phản hồi < 5ms; Kiểm thử hiệu năng đọc/ghi giỏ hàng.
    - *Phụ thuộc:* Order Service khởi tạo.

#### Ngày 7: Thứ Ba, 2026-11-03
19. **Lê Đức Anh** - `[Auth & OTP] Hiện thực cơ chế xác thực OTP qua Email với Redis TTL 5 phút và Rate-Limiting gửi mã`
    - *Phân hệ:* Auth & OTP
    - *Chi tiết:* Tích hợp Redis lưu trữ mã OTP kích hoạt tài khoản với thời gian hết hạn TTL 5 phút; xây dựng API gửi lại OTP có cơ chế Rate Limiting chống spam (tối thiểu 60s giữa 2 lần gửi).
    - *Đầu ra:* Luồng xác thực OTP hoàn chỉnh; Bộ test kiểm tra TTL hết hạn và xử lý nhập sai mã quá 5 lần.
    - *Phụ thuộc:* API đăng ký tài khoản.
20. **Triệu Quốc Bình** - `[Catalog Variants] Hiện thực quản lý biến thể SKU: Màu sắc, kích thước, định giá và kiểm soát tồn kho`
    - *Phân hệ:* Catalog Variants
    - *Chi tiết:* Hiện thực thực thể ProductSku: Quản lý biến thể đa thuộc tính, giá niêm yết, giá khuyến mãi, tồn kho thực tế (`stock`) và tồn kho đang giữ (`reserved_stock`); cài đặt khóa lạc quan `@Version`.
    - *Đầu ra:* API quản lý SKU đa biến thể; Ngăn chặn triệt để tình trạng âm kho (Negative Stock).
    - *Phụ thuộc:* Product CRUD.
21. **Nguyễn Văn Kiên** - `[Cart Merging] Hiện thực thuật toán hợp nhất giỏ hàng (Guest Cart -> User Cart) khi đăng nhập`
    - *Phân hệ:* Cart Merging
    - *Chi tiết:* Xây dựng logic tự động gộp giỏ hàng tạm của khách vãng lai vào giỏ hàng tài khoản khi người dùng đăng nhập; xử lý cộng dồn số lượng trùng SKU và giới hạn tồn kho khả dụng.
    - *Đầu ra:* Luồng merge cart trơn tru không mất dữ liệu; Bộ integration test kiểm tra logic gộp giỏ hàng.
    - *Phụ thuộc:* Redis Cart, SKU API.

#### Ngày 8: Thứ Tư, 2026-11-04
22. **Lê Đức Anh** - `[Security & JWT] Triển khai JWT Dual Auth (Cookie & Bearer), Refresh Token Rotation và Redis Blacklist`
    - *Phân hệ:* Security & JWT
    - *Chi tiết:* Xây dựng hệ thống sinh Access Token (15 phút) và Refresh Token (7 ngày); hỗ trợ lấy token đồng thời qua HttpOnly Cookie và Authorization Bearer header; lưu token bị thu hồi trên Redis Blacklist.
    - *Đầu ra:* API Đăng nhập, Làm mới Token và Đăng xuất an toàn; Middleware JWT Filter bảo vệ toàn diện.
    - *Phụ thuộc:* Identity Service, Redis.
23. **Triệu Quốc Bình** - `[MinIO Storage] Tích hợp MinIO S3 lưu trữ hình ảnh sản phẩm và xử lý chuẩn hóa tệp tin đa phương tiện`
    - *Phân hệ:* MinIO Storage
    - *Chi tiết:* Tích hợp SDK MinIO vào Catalog Service; hiện thực API tải lên hình ảnh sản phẩm (hỗ trợ PNG, JPG, WebP), kiểm tra dung lượng tối đa 5MB và sinh đường dẫn HTTPS stream URL công khai.
    - *Đầu ra:* API upload ảnh hoạt động ổn định; Lưu trữ và truy xuất ảnh sản phẩm mượt mà qua Nginx proxy.
    - *Phụ thuộc:* MinIO container.
24. **Nguyễn Văn Kiên** - `[Checkout Core] Xây dựng công cụ tính toán Checkout đa shop: Tách Sub-Orders và lưu trữ snapshot`
    - *Phân hệ:* Checkout Core
    - *Chi tiết:* Phát triển logic Checkout đa shop: Nhận danh sách SKU được tick chọn từ giỏ hàng, nhóm theo `shop_id` để tạo 1 Master Order và các Sub-Orders tương ứng; lưu trữ snapshot tên sản phẩm, giá bán tại thời điểm mua.
    - *Đầu ra:* API Checkout tạo Master/Sub-Orders thành công; Đảm bảo tính toàn vẹn dữ liệu đơn hàng.
    - *Phụ thuộc:* Cart Service, Order DB.

#### Ngày 9: Thứ Năm, 2026-11-05
25. **Lê Đức Anh** - `[Gateway RBAC] Thiết lập bộ lọc phân quyền Gateway: Kiểm soát truy cập BUYER, SELLER và ADMIN`
    - *Phân hệ:* Gateway RBAC
    - *Chi tiết:* Cấu hình ma trận phân quyền RBAC tại Gateway: Chặn truy cập trái phép vào các endpoint `/api/seller/**` và `/api/admin/**`; chuẩn hóa phản hồi lỗi 401 Unauthorized và 403 Forbidden.
    - *Đầu ra:* Bộ lọc Gateway RBAC chuẩn hóa; Bộ integration test kiểm tra 10 kịch bản truy cập trái quyền.
    - *Phụ thuộc:* JWT Authentication.
26. **Triệu Quốc Bình** - `[Catalog Cache] Tích hợp Redis Caching cho Catalog Service và cơ chế Invalidate Cache khi cập nhật kho`
    - *Phân hệ:* Catalog Cache
    - *Chi tiết:* Cấu hình Spring Cache với Redis cho các truy vấn xem danh mục, danh sách sản phẩm trang chủ và chi tiết sản phẩm; cài đặt cơ chế tự động xóa/cập nhật cache khi Seller chỉnh sửa sản phẩm hoặc khi đơn hàng trừ kho.
    - *Đầu ra:* Tốc độ phản hồi API Catalog giảm xuống < 10ms; Cache Invalidation hoạt động chính xác.
    - *Phụ thuộc:* Catalog API, Redis.
27. **Nguyễn Văn Kiên** - `[Voucher Engine] Hiện thực công cụ tính toán Voucher 2 tầng: Áp dụng đồng thời Voucher Sàn và Voucher Shop`
    - *Phân hệ:* Voucher Engine
    - *Chi tiết:* Xây dựng VoucherService: Hỗ trợ mã giảm giá theo tỷ lệ phần trăm (%), giảm tiền cố định và mã Freeship; cho phép áp dụng đồng thời 1 voucher sàn QuickShop và 1 voucher riêng của Shop; kiểm tra giá trị đơn tối thiểu.
    - *Đầu ra:* Engine tính toán giảm giá kép chuẩn xác; Bộ unit test kiểm tra điều kiện áp dụng và giới hạn số tiền giảm.
    - *Phụ thuộc:* Checkout Core.

#### Ngày 10: Thứ Sáu, 2026-11-06
28. **Lê Đức Anh** - `[Shop Onboarding] Xây dựng quy trình đăng ký gian hàng KYC và tạo hồ sơ Shop trạng thái PENDING_APPROVAL`
    - *Phân hệ:* Shop Onboarding
    - *Chi tiết:* Triển khai API đăng ký gian hàng bán hàng: Tiếp nhận thông tin định danh (Tên Shop, Mã số thuế, CCCD, địa chỉ kho lấy hàng, tài khoản ngân hàng thụ hưởng); lưu trữ với trạng thái chờ duyệt `PENDING_APPROVAL`.
    - *Đầu ra:* Bộ API đăng ký gian hàng và cập nhật hồ sơ Shop; Kiểm tra tính hợp lệ định dạng số giấy tờ pháp lý.
    - *Phụ thuộc:* Identity Service.
29. **Triệu Quốc Bình** - `[Buyer Storefront UI] Kết nối giao diện Buyer Catalog với API thật: Tìm kiếm, lọc giá và phân trang dữ liệu`
    - *Phân hệ:* Buyer Storefront UI
    - *Chi tiết:* Thay thế toàn bộ dữ liệu mock trên giao diện Buyer bằng API Catalog thật: Hoàn thiện thanh tìm kiếm tức thì, bộ lọc khoảng giá đa cấp, lọc theo danh mục con và thanh phân trang 12 sản phẩm/trang.
    - *Đầu ra:* Giao diện duyệt sản phẩm hoạt động mượt mà với dữ liệu thật từ cơ sở dữ liệu; Xử lý trạng thái loading/empty/error chuẩn chỉ.
    - *Phụ thuộc:* Catalog API & Cache.
30. **Nguyễn Văn Kiên** - `[Shipping Calculator] Khởi tạo Shipping Service (Port 8085); thuật toán tính cước vận chuyển GHN/GHTK linh hoạt`
    - *Phân hệ:* Shipping Calculator
    - *Chi tiết:* Khởi tạo Shipping Service; xây dựng công thức tính phí giao hàng dựa trên địa giới hành chính (nội tỉnh / liên tỉnh), trọng lượng kiện hàng (> 1000g) và hình thức giao hàng (Tiêu chuẩn / Hỏa tốc).
    - *Đầu ra:* Shipping Service chạy cổng 8085; API tính cước vận chuyển trả về kết quả chính xác theo đối tác vận chuyển.
    - *Phụ thuộc:* Order Service, common-library.

> 🛑 **NGHỈ CUỐI TUẦN:** Thứ Bảy, 07/11/2026 & Chủ Nhật, 08/11/2026 — Không làm việc.

---

### SPRINT 3: SAGA ORCHESTRATION, THANH TOÁN VNPAY, KÝ QUỸ ESCROW & AI GEMINI (09/11/2026 – 13/11/2026)

#### Ngày 11: Thứ Hai, 2026-11-09
31. **Lê Đức Anh** - `[Admin Approval] Hiện thực quy trình Admin phê duyệt gian hàng KYC, cấp quyền ROLE_SELLER và phát sự kiện`
    - *Phân hệ:* Admin Approval
    - *Chi tiết:* Xây dựng API dành cho Quản trị viên: Duyệt hoặc từ chối hồ sơ Shop kèm lý do; khi duyệt, tự động nâng cấp vai trò người dùng sang `ROLE_SELLER` và phát sự kiện `ShopApprovedEvent` qua RabbitMQ.
    - *Đầu ra:* API phê duyệt Shop hoàn chỉnh; Ghi nhận audit log lịch sử duyệt của Admin.
    - *Phụ thuộc:* Shop Onboarding, RabbitMQ.
32. **Triệu Quốc Bình** - `[Seller Center UI] Xây dựng Kênh Người Bán (Seller Center): Dashboard tổng quan và quản lý sản phẩm`
    - *Phân hệ:* Seller Center UI
    - *Chi tiết:* Phát triển giao diện Seller Center: Dashboard thống kê doanh số sơ bộ, bảng danh sách sản phẩm của gian hàng, bộ lọc trạng thái duyệt, form tạo mới/chỉnh sửa sản phẩm và tải ảnh lên MinIO.
    - *Đầu ra:* Giao diện Kênh Người Bán hoạt động hoàn chỉnh, liên kết dữ liệu với Catalog API.
    - *Phụ thuộc:* Product SKU API, MinIO Upload.
33. **Nguyễn Văn Kiên** - `[Saga Orchestrator] Thiết kế kiến trúc Saga Orchestration điều phối giao dịch phân tán qua RabbitMQ`
    - *Phân hệ:* Saga Orchestrator
    - *Chi tiết:* Thiết kế và triển khai Saga Orchestrator trong Order Service: Điều phối luồng đặt hàng phân tán qua RabbitMQ Exchange `order.events`: Khóa tồn kho (Reserve Stock) -> Khóa voucher -> Khởi tạo thanh toán -> Xác nhận đơn hàng.
    - *Đầu ra:* Sơ đồ trình tự Sequence Diagram luồng Saga; Module Saga Orchestrator xử lý chuỗi sự kiện không phong tỏa DB.
    - *Phụ thuộc:* Checkout Core, RabbitMQ.

#### Ngày 12: Thứ Ba, 2026-11-10
34. **Lê Đức Anh** - `[Notification Service] Khởi tạo Notification AI Service (Port 8086); lắng nghe sự kiện đơn hàng và gửi Email tự động`
    - *Phân hệ:* Notification Service
    - *Chi tiết:* Khởi tạo Notification AI Service; cấu hình RabbitMQ Consumer đón nhận sự kiện `OrderConfirmedEvent`; tích hợp JavaMailSender gửi email xác nhận chi tiết đơn hàng đến hộp thư của người mua.
    - *Đầu ra:* Notification Service chạy cổng 8086; Template email HTML hóa đơn đặt hàng gửi tự động và ghi nhật ký `email_logs`.
    - *Phụ thuộc:* RabbitMQ, Saga Orchestrator.
35. **Triệu Quốc Bình** - `[Product Reviews] Hiện thực hệ thống đánh giá sản phẩm 1-5 sao và tính điểm uy tín gian hàng`
    - *Phân hệ:* Product Reviews
    - *Chi tiết:* Phát triển phân hệ Đánh giá sản phẩm: Kiểm tra chặt chẽ điều kiện chỉ người mua có đơn hàng trạng thái `COMPLETED` mới được đánh giá, mỗi sản phẩm trong đơn chỉ đánh giá 1 lần; tự động tính điểm rating trung bình.
    - *Đầu ra:* API và giao diện đánh giá sản phẩm kèm ảnh và bình luận; Bộ lọc sản phẩm theo số sao.
    - *Phụ thuộc:* Catalog Service, Order status contract.
36. **Nguyễn Văn Kiên** - `[Saga Compensation & DLQ] Triển khai luồng bù trừ Saga Rollback và Dead Letter Queue (order.dlq)`
    - *Phân hệ:* Saga Compensation & DLQ
    - *Chi tiết:* Hiện thực cơ chế bù trừ khi phát sinh sự cố trong chuỗi Saga: Tự động hoàn lại hàng tồn kho (Release Stock), mở lại lượt dùng voucher và hủy đơn nếu thanh toán lỗi; cấu hình retry tối đa 3 lần và đẩy tin nhắn lỗi vào `order.dlq`.
    - *Đầu ra:* Luồng bồi hoàn Saga Rollback hoạt động hoàn hảo dưới 200ms; Cơ chế Dead Letter Queue giám sát lỗi tin nhắn.
    - *Phụ thuộc:* Saga Orchestrator.

#### Ngày 13: Thứ Tư, 2026-11-11
37. **Lê Đức Anh** - `[AI Assistant Core] Xây dựng Trợ lý ảo tư vấn mua sắm thông minh tích hợp Google Gemini API`
    - *Phân hệ:* AI Assistant Core
    - *Chi tiết:* Tích hợp Google Gemini API vào Notification AI Service; thiết kế System Prompt đóng vai trợ lý mua sắm QuickShop am hiểu sản phẩm, kiểm soát độ an toàn nội dung, chống Prompt Injection và lọc đầu ra độc hại.
    - *Đầu ra:* Endpoint Trợ lý AI chat thông minh; Bộ test kiểm thử tính an toàn của prompt và độ chính xác tư vấn.
    - *Phụ thuộc:* Notification Service.
38. **Triệu Quốc Bình** - `[Admin Portal UI] Xây dựng Cổng Quản Trị Hệ Thống (Admin Portal): Kiểm duyệt sản phẩm và quản lý người dùng`
    - *Phân hệ:* Admin Portal UI
    - *Chi tiết:* Xây dựng các màn hình quản trị cốt lõi: Bảng kiểm duyệt sản phẩm của người bán (Duyệt/Từ chối kèm lý do), bảng quản lý hồ sơ Shop KYC chờ duyệt, danh sách quản lý tài khoản người dùng và khóa tài khoản vi phạm.
    - *Đầu ra:* Giao diện Cổng Quản Trị Admin chuẩn responsive; Tích hợp đầy đủ các API phê duyệt của backend.
    - *Phụ thuộc:* Admin Approval API, Product API.
39. **Nguyễn Văn Kiên** - `[VNPay Payment] Tích hợp Cổng thanh toán trực tuyến VNPay Sandbox và xử lý VNPay IPN Idempotent`
    - *Phân hệ:* VNPay Payment
    - *Chi tiết:* Tích hợp Payment Service (Port 8084) với VNPay: Sinh URL thanh toán có chữ ký HMAC-SHA512 theo tiêu chuẩn tài liệu kỹ thuật VNPay; xử lý callback IPN bất đồng bộ, xác minh chữ ký số và chống xử lý trùng lặp giao dịch.
    - *Đầu ra:* Luồng thanh toán VNPay Sandbox hoàn chỉnh; Xử lý IPN Idempotent đảm bảo cập nhật trạng thái đơn chính xác tuyệt đối.
    - *Phụ thuộc:* Payment Service khởi tạo, Order Service.

#### Ngày 14: Thứ Năm, 2026-11-12
40. **Lê Đức Anh** - `[AI Function Calling] Tích hợp Gemini Function Calling tra cứu sản phẩm trực tiếp và Circuit Breaker chống cạn Quota`
    - *Phân hệ:* AI Function Calling
    - *Chi tiết:* Mở rộng khả năng AI với Function Calling: Trợ lý bot tự động gọi API tìm kiếm sản phẩm của Catalog Service dựa trên yêu cầu của khách; trang bị Resilience4j Circuit Breaker và cơ chế fallback trả về danh mục bán chạy khi hết quota API.
    - *Đầu ra:* Chatbot AI tư vấn kèm danh thiếp sản phẩm liên quan thực tế; Cơ chế Fallback hoạt động bền bỉ khi mất kết nối ngoài.
    - *Phụ thuộc:* AI Assistant Core, Catalog API.
41. **Triệu Quốc Bình** - `[Buyer Flow Integration] Hoàn thiện trải nghiệm giỏ hàng phân tách theo shop và quy trình Checkout`
    - *Phân hệ:* Buyer Flow Integration
    - *Chi tiết:* Tinh chỉnh giao diện giỏ hàng phong cách Shopee: Cho phép tick chọn sản phẩm theo từng Shop hoặc chọn tất cả, tự động tính tổng tiền và phí ship; hoàn thiện trang Checkout chọn địa chỉ nhận hàng và phương thức thanh toán.
    - *Đầu ra:* Luồng Mua sắm từ Giỏ hàng -> Checkout -> Thanh toán hoạt động trơn tru; Đồng bộ trạng thái giỏ hàng tức thì.
    - *Phụ thuộc:* Cart API, Checkout API.
42. **Nguyễn Văn Kiên** - `[Ledger & Escrow] Xây dựng Sổ cái kế toán kép (Double-entry Ledger) và Tài khoản ký quỹ trung gian Escrow`
    - *Phân hệ:* Ledger & Escrow
    - *Chi tiết:* Triển khai mô hình tài chính kế toán kép trong Payment Service: Quản lý tài khoản ký quỹ trung gian `ESCROW_HOLDING`; khi khách thanh toán thành công, đóng băng tiền hàng; bảo đảm mọi bút toán luôn cân bằng: Tổng Nợ (Debit) = Tổng Có (Credit).
    - *Đầu ra:* Module Sổ cái kép và Ký quỹ Escrow; Bộ unit test kiểm tra bất biến cân bằng kế toán (Accounting Invariant).
    - *Phụ thuộc:* VNPay Payment, Order DB.

#### Ngày 15: Thứ Sáu, 2026-11-13
43. **Lê Đức Anh** - `[AI SEO Generator] Phát triển tính năng Trí tuệ nhân tạo tự động viết mô tả sản phẩm chuẩn SEO cho Kênh Người Bán`
    - *Phân hệ:* AI SEO Generator
    - *Chi tiết:* Xây dựng endpoint AI trong Notification AI Service: Tiếp nhận thông số kỹ thuật, thương hiệu, tính năng sản phẩm từ Seller và sử dụng Gemini API tạo bài viết mô tả sản phẩm hấp dẫn, chuẩn cấu trúc SEO thương mại điện tử.
    - *Đầu ra:* Nút bấm "Tạo mô tả bằng AI" trong Seller Center hoạt động ấn tượng; Giảm 80% thời gian đăng bài cho người bán.
    - *Phụ thuộc:* AI Assistant Core, Seller Center.
44. **Triệu Quốc Bình** - `[Admin Finance UI] Xây dựng Dashboard Tài chính Quản trị viên: GMV, Doanh thu hoa hồng sàn và Quản lý rút tiền`
    - *Phân hệ:* Admin Finance UI
    - *Chi tiết:* Xây dựng màn hình tài chính cho Admin: Bảng biểu trực quan hiển thị tổng giá trị giao dịch (GMV), doanh thu hoa hồng sàn (Commission 5%), số dư tài khoản ký quỹ Escrow và danh sách các yêu cầu rút tiền của Seller chờ phê duyệt.
    - *Đầu ra:* Giao diện Quản trị Tài chính Admin sắc nét; Thao tác duyệt yêu cầu rút tiền của người bán cập nhật số dư tức thì.
    - *Phụ thuộc:* Ledger & Escrow, Admin Portal UI.
45. **Nguyễn Văn Kiên** - `[Shipping Webhooks] Hoàn thiện tích hợp Shipping Service: Sinh mã vận đơn và xử lý Webhook Idempotent theo event_id`
    - *Phân hệ:* Shipping Webhooks
    - *Chi tiết:* Hoàn thiện luồng giao vận: Tạo mã vận đơn chuẩn hóa (`GHN-XXXX`, `GHTK-XXXX`), cung cấp endpoint Simulator cập nhật lộ trình; xử lý Webhook từ đối tác giao vận đảm bảo tính Idempotency dựa trên `event_id` chống xử lý lặp sự kiện.
    - *Đầu ra:* Bộ API vận chuyển hoàn thiện; Kiểm thử nhận lặp 5 lần cùng 1 webhook vẫn bảo đảm dữ liệu đơn hàng chính xác.
    - *Phụ thuộc:* Shipping Calculator, Order Service.

> 🛑 **NGHỈ CUỐI TUẦN:** Thứ Bảy, 14/11/2026 & Chủ Nhật, 15/11/2026 — Không làm việc.

---

### SPRINT 4: MỞ RỘNG TÍNH NĂNG CAO CẤP, KÊNH SHIPPER, PAYOS VIETQR, MAP LEAFLET, CHAT, QA & BÀN GIAO (16/11/2026 – 20/11/2026)

#### Ngày 16: Thứ Hai, 2026-11-16
46. **Lê Đức Anh** - `[UI Hardening & Motion] Nâng cấp giao diện theo chuẩn Impeccable: Cuộn mượt Lenis và chuyển động chuyên nghiệp GSAP`
    - *Phân hệ:* UI Hardening & Motion
    - *Chi tiết:* Nâng cấp toàn diện trải nghiệm thị giác giao diện Storefront: Tích hợp thư viện cuộn mượt Lenis; cấu hình kiến trúc chuyển động GSAP (`AnimatedPresence`, `PageTransition`, `StaggerReveal`); tối ưu hiệu năng tránh giật lag khung hình.
    - *Đầu ra:* Trải nghiệm lướt web mượt mà 60fps; Triệt tiêu cảnh báo GSAP Scope và tối ưu hóa vòng đời canvas.
    - *Phụ thuộc:* Buyer Storefront UI.
47. **Triệu Quốc Bình** - `[Redesign Flagship] Thiết kế lại giao diện Flagship: Bento Hero Grid, Floating Navbar và Modal bộ lọc trung tâm`
    - *Phân hệ:* Redesign Flagship
    - *Chi tiết:* Tái thiết kế giao diện theo phong cách Flagship hiện đại: Bố cục Bento Hero Grid giới thiệu bộ sưu tập nổi bật, thanh điều hướng ghim nổi Glassmorphism (Floating Navbar) và chuyển bảng bộ lọc thành Modal Dialog căn giữa màn hình.
    - *Đầu ra:* Giao diện Storefront đẳng cấp, hiện đại, thân thiện với người dùng; Tương thích hoàn hảo mọi độ phân giải.
    - *Phụ thuộc:* UI Hardening & Motion.
48. **Nguyễn Văn Kiên** - `[Rich Text Editor] Tích hợp CKEditor 5 và tải ảnh nội tuyến trực tiếp lên MinIO cho mô tả sản phẩm`
    - *Phân hệ:* Rich Text Editor
    - *Chi tiết:* Tích hợp trình soạn thảo văn bản giàu tính năng CKEditor 5 vào Kênh Người Bán; xây dựng adapter upload ảnh nội tuyến trực tiếp lên bucket MinIO, tự động sinh thẻ ảnh trong nội dung mô tả HTML sản phẩm.
    - *Đầu ra:* Trình soạn thảo mô tả sản phẩm chuyên nghiệp cho Seller; Ảnh mô tả được lưu trữ an toàn và hiển thị đẹp mắt.
    - *Phụ thuộc:* MinIO Storage, Seller Center UI.

#### Ngày 17: Thứ Ba, 2026-11-17
49. **Lê Đức Anh** - `[3D Showcase] Xây dựng trang giới thiệu tương tác 3D Scrollytelling 6 phân hệ (WebGL/Canvas) và triệt tiêu lỗi phông chữ tiếng Việt`
    - *Phân hệ:* 3D Showcase
    - *Chi tiết:* Phát triển trang giới thiệu IntroPage dạng Scrollytelling 3D trình diễn 6 phân hệ của QuickShop bằng Three.js/WebGL; kiểm tra toàn bộ bảng mã cơ sở dữ liệu và xử lý triệt để lỗi hiển thị font tiếng Việt Mojibake UTF-8.
    - *Đầu ra:* Trang giới thiệu 3D ấn tượng đạt chuẩn công nghệ cao; Toàn bộ tên sản phẩm, biến thể và mô tả hiển thị tiếng Việt chuẩn 100%.
    - *Phụ thuộc:* UI Hardening & Motion.
50. **Triệu Quốc Bình** - `[Real-time Messenger] Xây dựng giao diện Chat toàn màn hình (Full-page Messenger) giữa Người mua và Người bán`
    - *Phân hệ:* Real-time Messenger
    - *Chi tiết:* Phát triển phân hệ Chat thời gian thực Buyer - Seller phong cách Shopee Chat / Facebook Messenger: Giao diện danh sách cuộc trò chuyện bên trái, khung hội thoại bên phải, nhúng thẻ sản phẩm thông minh đang xem và gửi tin nhắn tức thì.
    - *Đầu ra:* Hệ thống Messenger trực tuyến đầy đủ tính năng; Hỗ trợ người mua chat trực tiếp với gian hàng để thương lượng và tư vấn.
    - *Phụ thuộc:* Storefront Layout, Auth Store.
51. **Nguyễn Văn Kiên** - `[PayOS VietQR] Tích hợp Cổng thanh toán chuyển khoản ngân hàng tự động PayOS chuẩn EMVCo VietQR Code`
    - *Phân hệ:* PayOS VietQR
    - *Chi tiết:* Tích hợp cổng PayOS vào Payment Service: Tự động sinh mã VietQR chuẩn EMVCo động chứa số tiền và nội dung chuyển khoản theo mã đơn hàng; xử lý webhook thanh toán thành công và cung cấp nút bấm đối soát thanh toán thủ công.
    - *Đầu ra:* Phương thức thanh toán chuyển khoản ngân hàng qua quét mã QR hoạt động tức thì; Trải nghiệm thanh toán không tiền mặt tiện lợi.
    - *Phụ thuộc:* Payment Service, VNPay Payment.

#### Ngày 18: Thứ Tư, 2026-11-18
52. **Lê Đức Anh** - `[Security & QA Hardening] Khắc phục toàn diện 18 lỗi kiểm thử bảo mật QA, gia cố chữ ký JWT Base64URL và Nginx Headers`
    - *Phân hệ:* Security & QA Hardening
    - *Chi tiết:* Rà soát và vá triệt để 18 lỗi ma trận QA: Kiểm tra chữ ký Base64URL canonical của JWT, chống tấn công SQLi/IDOR, bật bảo vệ Nginx Security Headers (CSP, X-Frame-Options, HSTS), loại bỏ thông tin nhạy cảm và tối ưu bộ nhớ JVM trên VPS 1GB RAM.
    - *Đầu ra:* Toàn bộ 18 tiêu chí an ninh QA Passed 100%; Hệ thống vận hành an toàn, tiết kiệm tài nguyên.
    - *Phụ thuộc:* API Gateway, Deployment configs.
53. **Triệu Quốc Bình** - `[Order Cards & Avatar] Nâng cấp hiển thị Sub-Orders tách biệt, cập nhật ảnh đại diện Avatar và lịch sử đơn hàng`
    - *Phân hệ:* Order Cards & Avatar
    - *Chi tiết:* Nâng cấp trang Lịch sử đơn hàng: Tách biệt các đơn hàng con (Sub-Orders) theo từng thẻ riêng biệt kèm logo shop; bổ sung Modal xem chi tiết lộ trình vận chuyển; hoàn thiện tính năng người dùng cập nhật ảnh đại diện (Avatar).
    - *Đầu ra:* Giao diện quản lý đơn hàng minh bạch, trực quan; Người dùng dễ dàng theo dõi từng gói hàng của các shop khác nhau.
    - *Phụ thuộc:* Buyer Flow Integration, MinIO Storage.
54. **Nguyễn Văn Kiên** - `[Escrow Auto-Release & Stats] Triển khai tự động giải phóng tiền Escrow sau 72h và Bảng thống kê doanh thu theo SKU`
    - *Phân hệ:* Escrow Auto-Release & Stats
    - *Chi tiết:* Xây dựng Scheduled Task tự động chuyển trạng thái tiền ký quỹ sang ví khả dụng của Người bán sau 72 giờ kể từ khi đơn hàng giao thành công; bổ sung API thống kê doanh thu chi tiết theo từng mã sản phẩm SKU cho Kênh Người Bán.
    - *Đầu ra:* Quy trình thanh toán tự động hóa hoàn toàn; Bảng báo cáo doanh thu theo SKU giúp người bán nắm rõ mặt hàng sinh lời.
    - *Phụ thuộc:* Ledger & Escrow, Shipping Webhooks.

#### Ngày 19: Thứ Năm, 2026-11-19
55. **Lê Đức Anh** - `[Shipper Module] Triển khai phân hệ Nhân viên giao hàng (ROLE_SHIPPER): Mở rộng Identity và API vận chuyển bưu tá`
    - *Phân hệ:* Shipper Module
    - *Chi tiết:* Mở rộng Identity Service hỗ trợ vai trò `ROLE_SHIPPER`: Khởi tạo tài khoản bưu tá mặc định (`shipper@quickshop.vn`), xây dựng trang đăng ký và đăng nhập dành riêng cho bưu tá; liên kết quyền hạn với hệ thống vận chuyển.
    - *Đầu ra:* Tài khoản và quyền hạn Shipper sẵn sàng; Bộ API phục vụ tiếp nhận đơn và cập nhật trạng thái kiện hàng cho bưu tá.
    - *Phụ thuộc:* Identity Service, Shipping Service.
56. **Triệu Quốc Bình** - `[Shipper Portal UI] Xây dựng Cổng thông tin Nhân viên giao hàng (Shipper Portal) và Thẻ sản phẩm Chatbot AI`
    - *Phân hệ:* Shipper Portal UI
    - *Chi tiết:* Phát triển giao diện Kênh Giao Hàng dành cho Shipper: Danh sách đơn hàng cần lấy (`READY_TO_PICK`), danh sách đang giao (`DELIVERING`), nút bấm xác nhận đã giao thành công; đồng thời bổ sung thẻ sản phẩm tương tác vào Chatbot AI.
    - *Đầu ra:* Cổng Shipper Portal hoạt động trên di động và máy tính; Chatbot AI hiển thị thẻ sản phẩm trực quan có thể click xem ngay.
    - *Phụ thuộc:* Shipper Module, AI Assistant Core.
57. **Nguyễn Văn Kiên** - `[Flash Sale Manager] Xây dựng Cổng quản trị Flash Sale đa chế độ (Manual & AI Mode) kèm bộ đếm ngược đồng bộ`
    - *Phân hệ:* Flash Sale Manager
    - *Chi tiết:* Phát triển hệ thống Flash Sale toàn diện: Cho phép Admin thiết lập khung giờ giảm giá sốc, hỗ trợ chọn sản phẩm thủ công hoặc kích hoạt chế độ AI tự động phân tích và duyệt sản phẩm; tích hợp bộ đếm ngược thời gian thực trên Storefront.
    - *Đầu ra:* Phân hệ Flash Sale hoạt động sống động trên trang chủ; Kích thích chuyển đổi mua hàng với đồng hồ đếm ngược.
    - *Phụ thuộc:* Catalog Service, Order Service.

#### Ngày 20: Thứ Sáu, 2026-11-20 (NGÀY NGHIỆM THU & BÀN GIAO TOÀN DIỆN)
58. **Lê Đức Anh** - `[Google OAuth & Map Proxy] Tích hợp Đăng nhập Google OAuth, Bản đồ số Leaflet OpenStreetMap và CI/CD hoàn tất`
    - *Phân hệ:* Google OAuth & Map Proxy
    - *Chi tiết:* Hoàn tất tích hợp Google OAuth 2.0 (đăng ký/đăng nhập/khôi phục mật khẩu); tích hợp bản đồ Leaflet OpenStreetMap và cấu hình Nginx Tile Cache & Nominatim Geocoding proxy tránh Mixed Content; đóng gói Docker Compose và deploy VPS thành công.
    - *Đầu ra:* Đăng nhập Google một chạm mượt mà; Bản đồ chọn địa chỉ giao hàng trực quan có định vị GPS; Hệ thống sẵn sàng trên production.
    - *Phụ thuộc:* Identity Service, Nginx, DevOps.
59. **Triệu Quốc Bình** - `[QA Frontend & Handover] Tổng duyệt kiểm thử giao diện E2E 4 phân hệ, tối ưu Responsive và hoàn thiện User Guide`
    - *Phân hệ:* QA Frontend & Handover
    - *Chi tiết:* Kiểm thử hồi quy toàn diện giao diện trên các thiết bị (Desktop, Laptop, Tablet iPad, Mobile iOS/Android); nghiệm thu 4 phân hệ (Buyer, Seller, Admin, Shipper); cập nhật tài liệu Hướng dẫn sử dụng `USER_GUIDE.md` đầy đủ hình ảnh và tính năng mới.
    - *Đầu ra:* Giao diện đạt chuẩn 100% không lỗi hiển thị hay vỡ khung; Tài liệu Hướng dẫn sử dụng hoàn thiện bàn giao cho khách hàng.
    - *Phụ thuộc:* Toàn bộ các phân hệ Frontend.
60. **Nguyễn Văn Kiên** - `[QA Backend & Regression] Tổng duyệt kiểm thử hồi quy Backend, kiểm tra Sổ cái kép và ký Biên bản bàn giao kỹ thuật`
    - *Phân hệ:* QA Backend & Regression
    - *Chi tiết:* Chạy toàn bộ test suites của 7 microservices (`./gradlew test` đạt 100%); kiểm tra luồng Saga Rollback, đối soát số dư ví ký quỹ Escrow, tính chính xác của thanh toán PayOS/VNPay và chốt Báo cáo kiểm thử hồi quy `BACKEND_REGRESSION_REPORT.md`.
    - *Đầu ra:* Báo cáo kiểm thử Backend hoàn chỉnh (100% Passed); Biên bản nghiệm thu kỹ thuật và phân công bảo hành dự án.
    - *Phụ thuộc:* Toàn bộ các phân hệ Backend.

---

## 5. Ma trận phân bổ trách nhiệm (RACI Matrix)

*R: Responsible (Người thực hiện chính) | A: Accountable (Người chịu trách nhiệm kết quả) | C: Consulted (Người tư vấn/phối hợp) | I: Informed (Người nhận thông tin)*

| Hạng mục / Phân hệ chức năng | Lê Đức Anh | Nguyễn Văn Kiên | Triệu Quốc Bình |
| :--- | :---: | :---: | :---: |
| **Kiến trúc hệ thống, API Gateway & Nginx Edge** | **A / R** | C | I |
| **Bảo mật JWT, RBAC, Google OAuth, Email OTP** | **A / R** | C | C |
| **Hạ tầng Docker, CI/CD Actions, Monitoring (Prometheus/Grafana)** | **A / R** | I | I |
| **Trí tuệ nhân tạo Gemini (Chatbot & AI SEO Generator)** | **A / R** | I | C |
| **Mô hình Dữ liệu, Liquibase/Flyway & JPA Repositories** | C | **A / R** | C |
| **Giỏ hàng phân tán đa shop trên Redis (Cart Service)** | C | **A / R** | C |
| **Checkout Engine & Saga Orchestrator (RabbitMQ & DLQ)** | C | **A / R** | C |
| **Thanh toán VNPay Sandbox & PayOS VietQR EMVCo** | I | **A / R** | C |
| **Sổ cái kế toán kép (Double-entry Ledger) & Ký quỹ Escrow** | I | **A / R** | I |
| **Vận chuyển GHN/GHTK & Webhook Idempotency theo event_id** | I | **A / R** | C |
| **Cổng quản trị Flash Sale đa chế độ (Manual & AI Mode)** | C | **A / R** | C |
| **Thiết kế UI/UX, Design System Impeccable & Tailwind** | C | I | **A / R** |
| **Cổng Người Mua (Buyer Storefront) & Trình lọc sản phẩm** | I | C | **A / R** |
| **Kênh Người Bán (Seller Center) & Tích hợp CKEditor 5** | I | C | **A / R** |
| **Cổng Quản Trị Hệ Thống (Admin Portal & Phê duyệt)** | C | C | **A / R** |
| **Kênh Nhân viên Giao Hàng (Shipper Portal)** | C | C | **A / R** |
| **Bản đồ Leaflet OSM tương tác & Nominatim GPS Geocoding** | C | I | **A / R** |
| **Hệ thống Tin nhắn Thời gian thực (Full-page Messenger)** | I | I | **A / R** |
| **Trang Giới thiệu 3D Scrollytelling Showcase (WebGL/GSAP)** | C | I | **A / R** |
| **Kiểm thử hồi quy Backend (QA Backend Integration)** | C | **A / R** | I |
| **Kiểm thử hồi quy Frontend (QA Frontend Responsive E2E)** | I | I | **A / R** |

---

## 6. Thống kê khối lượng & Tiêu chí nghiệm thu hệ thống

### 6.1. Thống kê định lượng toàn dự án
- **Thời gian thực hiện:** 26/10/2026 – 20/11/2026 (**4 Sprint** – **20 ngày làm việc**, nghỉ Thứ 7 và Chủ Nhật).
- **Tổng số công việc phân công chi tiết:** **60 đầu việc** (chia đều chính xác 20 đầu việc/thành viên).
- **Tổng số Git Commits:** **389 commits** hoàn thiện trên kho mã nguồn với ghi chú tiếng Việt chuẩn mực.
- **Số lượng Microservices:** **7 services** độc lập (`api-gateway`, `identity-service`, `catalog-service`, `order-service`, `payment-service`, `shipping-service`, `notification-ai-service`) kèm `common-library`.
- **Số lượng Phân hệ Người dùng Frontend:** **4 phân hệ** hoàn chỉnh (`Buyer Portal`, `Seller Center`, `Admin Portal`, `Shipper Portal`) cùng `Intro 3D Showcase` và `Full-page Messenger`.
- **Số lượng Test Suites Backend:** 100% Passed (> 120 integration & unit tests, zero critical bug).
- **Tiêu chuẩn An toàn thông tin:** Vượt qua 18/18 tiêu chí kiểm thử bảo mật khắt khe.

### 6.2. Hướng dẫn đồng bộ dữ liệu vào Trello
Tệp tin `QuickShop_Trello_Import_Updated.csv` tại thư mục gốc của dự án đã được cập nhật chuẩn cấu trúc bảng Trello (bao gồm các cột `List`, `Name`, `Description`, `Start Date`, `Due Date`).
- **Bước 1:** Đăng nhập vào Trello Workspace của nhóm dự án.
- **Bước 2:** Chọn chức năng *Menu* -> *More* -> *Import* (hoặc sử dụng Power-Up *CSV Import*).
- **Bước 3:** Tải tệp `QuickShop_Trello_Import_Updated.csv` lên. Toàn bộ 60 thẻ công việc thuộc 4 Sprint sẽ được tự động phân bổ vào cột danh sách `Đã xong` với đầy đủ mô tả chi tiết, phân hệ, ngày bắt đầu và ngày hoàn thành từ **26/10/2026 đến 20/11/2026**.

---
*Báo cáo được hoàn thiện và phê duyệt ngày 20/11/2026 bởi Ban Kỹ thuật Dự án QuickShop.*
