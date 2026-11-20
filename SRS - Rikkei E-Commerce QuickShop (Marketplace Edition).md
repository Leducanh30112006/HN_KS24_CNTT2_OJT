# **ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS) - RIKKEI E-COMMERCE QUICKSHOP (MARKETPLACE EDITION)** 

**Sàn Thương mại Điện tử Đa người bán (Multi-Vendor Marketplace) - Microservices & DevOps Production-Ready** 

# **0. Lịch sử thay đổi tài liệu (Document Revision History)** 

| Phiên bản | Ngày | Tác giả | Tóm tắt thay đổi |
|---|---|---|---|
| 1.0.0 | 15/10/2026 | Lê Đức Anh | Khởi tạo tài liệu đặc tả ban đầu. |
| 2.0.0 | 21/10/2026 | Lê Đức Anh | Nâng cấp kiến trúc Microservices & chuẩn hóa DevOps. |
| 2.1.0 | 21/10/2026 | Lê Đức Anh | Bổ sung State Machine và thiết kế luồng Saga Orchestrator. |
| 2.2.0 | 21/10/2026 | Lê Đức Anh | Chuẩn hóa toàn diện: Thống nhất mô hình 7 services (1 Gateway + 6 Backend), chuẩn hóa cổng dịch vụ, bổ sung các bảng CSDL, hoàn thiện State Machine và Dual Auth. |
| 2.3.0 | 02/11/2026 | Toàn nhóm | Cập nhật Phân công Công việc & Tiến độ (WBS) giai đoạn 1. |
| **3.0.0** | **20/11/2026** | **Toàn nhóm** | **Nghiệm thu toàn diện 4 Sprint (kết thúc 20/11/2026, nghỉ T7 & CN):** Bổ sung phân hệ Kênh Shipper (`ROLE_SHIPPER`), cổng thanh toán PayOS VietQR EMVCo, Bản đồ số Leaflet OpenStreetMap & Nominatim GPS, Đăng nhập Google OAuth 2.0 & OTP Email qua Redis, Hệ thống Chat Full-page Messenger, Quản trị Flash Sale AI Auto-Approve, Trình soạn thảo CKEditor 5 MinIO, và Trang 3D Scrollytelling Showcase. Hoàn thành 100% tiêu chí nghiệm thu. |

---

# **1. Giới thiệu** 

## **1.1 Mục đích** 
Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này xác định toàn bộ các yêu cầu chức năng, phi chức năng, kiến trúc kỹ thuật và phương án triển khai cho sàn thương mại điện tử đa người bán (Multi-vendor Marketplace) **QuickShop (Marketplace Edition)** theo mô hình hoạt động tương tự Shopee. Hệ thống được thiết kế theo chuẩn mã nguồn mở cấp độ Production, đảm bảo khả năng chịu tải cao, mở rộng linh hoạt theo kiến trúc Microservices (Database-per-Service) và tự động hóa toàn diện quy trình vận hành qua DevOps/CI-CD.

## **1.2 Phạm vi sản phẩm & Chuẩn hóa Thuật ngữ** 
Hệ thống cung cấp nền tảng kết nối toàn diện cho 4 nhóm đối tượng chính:

- **Khách hàng (Buyer):** Tìm kiếm/lọc sản phẩm đa gian hàng qua Bento Grid & Modal trung tâm, chọn địa chỉ trực quan trên bản đồ Leaflet OpenStreetMap với GPS Geocoding, quản lý giỏ hàng tách theo Shop, áp dụng Voucher kép (Sàn & Shop), thanh toán đa phương thức (PayOS VietQR EMVCo, VNPay Sandbox, COD), theo dõi lộ trình bưu tá thời gian thực, trò chuyện trực tiếp với Shop qua Full-page Messenger kèm Thẻ sản phẩm thông minh, tương tác AI Chatbot tư vấn, đánh giá/nhận xét gian hàng và yêu cầu Đổi trả/Hoàn tiền (Refund/Return). 
- **Người bán (Seller):** Đăng ký và quản lý hồ sơ Shop KYC, quản lý danh mục & sản phẩm đa biến thể SKU, soạn thảo mô tả chi tiết bằng CKEditor 5 tải ảnh MinIO trực tiếp, tự động tạo mô tả SEO bằng AI, tiếp nhận và xử lý luồng đơn hàng con (Sub-Orders), kết nối đơn vị vận chuyển (GHN/GHTK), quản lý Ví Seller/Sổ cái dòng tiền kép (Double-Entry Ledger), xem bảng phân tích doanh thu theo từng mã SKU, đối soát tài chính và rút tiền về tài khoản ngân hàng. 
- **Quản trị viên (Admin):** Duyệt/Khóa hồ sơ mở Shop, kiểm duyệt sản phẩm mới của Seller, quản lý chiến dịch Flash Sale đa chế độ (Manual & AI Auto-Approve) kèm đồng hồ đếm ngược, quản lý cây danh mục chung, điều phối và phân xử tranh chấp hoàn tiền, cấu hình mức phí hoa hồng sàn (Commission 5%), quản lý Voucher toàn sàn, duyệt chi rút tiền, tra cứu Audit Log và theo dõi báo cáo doanh thu/dòng tiền Marketplace. 
- **Nhân viên Giao hàng (Shipper / Logistics):** Đăng nhập cổng Shipper Portal chuyên biệt, tiếp nhận kiện hàng cần lấy (`READY_TO_PICK`), cập nhật lộ trình giao hàng (`DELIVERING`), cập nhật tiến độ vận chuyển và xác nhận giao hàng thành công (`DELIVERED`) kích hoạt cơ chế Escrow 72h.
- **Vận hành (Ops/System):** Tích hợp hạ tầng lưu trữ Object Storage (MinIO S3), xác thực an toàn chuẩn JWT Dual Auth (HttpOnly Cookie & Bearer Header) / RBAC 4 vai trò, quản lý webhook idempotency kết nối đối tác vận chuyển, chạy các tác vụ định kỳ ShedLock, tự động hóa CI/CD triển khai Zero-Downtime và giám sát Log/Metrics/Tracing tập trung (Prometheus, Grafana, Zipkin).

## **1.2.1 Chuẩn hóa Trạng thái & State Machine Toàn hệ thống** 

- **Trạng thái Người dùng (User Status):** 
  `PENDING_VERIFICATION` (Chờ kích hoạt OTP) $\rightarrow$ `ACTIVE` (Đang hoạt động) $\rightarrow$ `BLOCKED` (Khóa tài khoản do vi phạm).
- **Trạng thái Gian hàng (Shop Status):** 
  `PENDING_APPROVAL` (Chờ Admin duyệt) $\rightarrow$ `ACTIVE` (Đang hoạt động kinh doanh) $\rightarrow$ `SUSPENDED` (Tạm dừng/Khóa shop) / `REJECTED` (Từ chối duyệt hồ sơ).
- **State Machine Master Order (Đơn hàng Tổng):** 
  `DRAFT` $\rightarrow$ `PENDING_PAYMENT` $\rightarrow$ `PROCESSING` $\rightarrow$ `PARTIALLY_COMPLETED` / `COMPLETED` / `CANCELLED` / `REFUNDED`.
- **State Machine Sub-Order (Đơn con theo từng Shop):** 
  `CREATED` $\rightarrow$ `CONFIRMED` (Shop xác nhận) $\rightarrow$ `PACKING` (Đang đóng gói) $\rightarrow$ `SHIPPING` (Đang giao hàng) $\rightarrow$ `DELIVERED` (Giao thành công) $\rightarrow$ `COMPLETED` (Đã giải phóng tiền Escrow vào ví Seller).
  * *Nhánh hoàn / hủy / khiếu nại:*
    * `CANCELLED`: Khách hủy khi chưa giao hoặc quá hạn thanh toán 15 phút.
    * `DELIVERY_FAILED`: Giao thất bại / Bom hàng $\rightarrow$ chuyển hoàn kho Shop.
    * `REFUNDING` $\rightarrow$ `REFUNDED`: Khách khiếu nại đổi trả được chấp thuận, kích hoạt hoàn tiền và đóng băng Escrow.
- **State Machine Vận đơn Shipper (Shipment Status):**
  `READY_TO_PICK` (Chờ shipper nhận lấy hàng) $\rightarrow$ `PICKED` (Đã lấy hàng từ kho) $\rightarrow$ `DELIVERING` (Đang vận chuyển giao khách) $\rightarrow$ `DELIVERED` (Đã giao thành công) / `DELIVERY_FAILED` (Giao không thành công) $\rightarrow$ `RETURNED` (Đã chuyển hoàn kho).

---

## **1.3 Công nghệ sử dụng** 

| Tầng kiến trúc | Công nghệ áp dụng | Vai trò & Chi tiết triển khai |
|---|---|---|
| **Frontend** | React 18, Vite, Zustand, Tailwind CSS, Lucide Icons, GSAP, Lenis, Three.js, Leaflet, CKEditor 5 | SPA Storefront (Bento Grid) + Seller Center + Admin Dashboard + Shipper Portal, hiệu ứng chuyển động 3D Scrollytelling & cuộn mượt Impeccable |
| **Backend API** | Spring Boot 3.3.x, Java 17 | Xây dựng 7 Microservices độc lập, Liquibase/Flyway migration, ShedLock cho Scheduled Tasks |
| **Microservices Framework** | Spring Cloud Gateway, OpenFeign, Resilience4j | API Routing, Rate Limiting (Token Bucket), Circuit Breaker, Inter-service Communication |
| **Security & Auth** | Spring Security 6, JWT Dual Auth (Cookie & Bearer), Google OAuth 2.0, BCrypt | Phân quyền RBAC 4 vai trò (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`, `ROLE_SHIPPER`), chống brute-force & SQLi fast-reject |
| **Database** | MySQL 8.0 (Database per Service) | 6 Database độc lập cho các backend services (`identity_db`, `catalog_db`, `order_db`, `payment_db`, `shipping_db`, `notification_db`) |
| **Caching & Messaging** | Redis 7, RabbitMQ 3.13 | Caching giỏ hàng/sản phẩm, OTP TTL 5 phút, Distributed Lock, Async Messaging (Saga Orchestrator Pattern, DLQ `order.dlq`) |
| **Thanh toán & Cổng QR** | VNPay Sandbox, PayOS VietQR EMVCo | Hỗ trợ thanh toán cổng VNPAY, quét mã VietQR động tự động khớp số tiền, Escrow tạm giữ và Sổ cái kép |
| **Bản đồ & Định vị** | Leaflet OpenStreetMap, Nominatim Geocoding | Bản đồ chọn địa chỉ trực quan, trích xuất tọa độ GPS, Nginx Tile Cache Proxy |
| **Storage & Static** | MinIO (S3 Compatible) | Object Storage cho hình ảnh sản phẩm, media gian hàng, avatar, ảnh nội tuyến CKEditor 5 |
| **DevOps & Infra** | Docker, Docker Compose, Nginx, GitHub Actions | Containerization tối ưu RAM cho VPS, Reverse Proxy SSL, CI/CD Rolling Deployment |
| **Monitoring & Tracing** | Prometheus, Grafana, Micrometer/Zipkin | Giám sát chỉ số tài nguyên hệ thống, tracing phân tán qua Correlation ID |

---

# **2. Kiến trúc Hệ thống (Microservices Architecture)** 

Hệ thống Marketplace được thiết kế theo mô hình Microservices phân tán với **1 API Gateway và 6 Dịch vụ Backend cốt lõi** cùng thư viện dùng chung `common-library`:

1. **API Gateway (Port 8080):** 
   Cổng giao tiếp duy nhất cho Storefront, Seller Center, Admin Portal và Shipper Portal. Đảm nhận API Routing, kiểm tra JWT Token (hỗ trợ trích xuất cả từ HttpOnly Cookie lẫn Bearer Header), phân quyền RBAC 4 vai trò (`BUYER`, `SELLER`, `ADMIN`, `SHIPPER`), proxy bản đồ OSM Tile & Nominatim Geocoding, áp dụng Rate Limiting chống DDoS dựa trên header `CF-Connecting-IP`.
2. **Identity & Shop Service (Port 8081):** 
   Quản lý tài khoản đa vai trò, tích hợp Google OAuth 2.0, OTP Email kích hoạt tài khoản với Redis TTL 5 phút, quy trình KYC/Đăng ký Gian hàng, phê duyệt hồ sơ Seller, phân quyền RBAC và quản lý thông tin Shipper. 
   *Cơ sở dữ liệu:* `identity_db`.
3. **Catalog & Product Service (Port 8082):** 
   Quản lý cây danh mục đa cấp, đăng bán sản phẩm thuộc gian hàng, kiểm duyệt bài đăng, biến thể SKU, quản lý tồn kho với khóa lạc quan `@Version`, upload media lên MinIO S3, tích hợp CKEditor 5 và đánh giá nhận xét sản phẩm (`product_reviews`). 
   *Cơ sở dữ liệu:* `catalog_db`.
4. **Order & Cart Service (Port 8083):** 
   Quản lý giỏ hàng đa Shop trên Redis, tính toán giá trị đơn, áp dụng Voucher kép (Sàn + Shop), tách đơn hàng tự động thành 1 Master Order và các Sub-Orders theo từng Shop. Đóng vai trò **Saga Orchestrator** điều phối toàn bộ chuỗi giao dịch phân tán qua RabbitMQ kèm cơ chế Dead Letter Queue (`order.dlq`). Quản lý yêu cầu đổi trả/hoàn tiền (`refund_requests`) và tự động hủy đơn quá hạn sau 15 phút. 
   *Cơ sở dữ liệu:* `order_db`.
5. **Payment & Wallet Service (Port 8084):** 
   Tích hợp đa cổng thanh toán: VNPay Sandbox (IPN & Return) và PayOS VietQR EMVCo. Quản lý Tài khoản Tạm giữ (Escrow) của Sàn, duy trì Sổ cái kép (Double-Entry Ledger) cho Ví Seller, tự động khấu trừ hoa hồng sàn (Commission Fee 5%), quản lý lệnh rút tiền (`withdrawals`) và tự động giải phóng tiền ví sau 72h đơn hoàn tất. 
   *Cơ sở dữ liệu:* `payment_db`.
6. **Shipping & Logistics Service (Port 8085):** 
   Tích hợp API đơn vị vận chuyển (GHN/GHTK hoặc Mock Logistics Simulator), tính phí ship chuẩn xác theo địa chỉ và khối lượng, sinh mã vận đơn tự động, cung cấp API cho Shipper Portal tiếp nhận/giao hàng và tiếp nhận Idempotent Webhook cập nhật hành trình dựa trên `event_id`. 
   *Cơ sở dữ liệu:* `shipping_db`.
7. **Notification & AI Service (Port 8086):** 
   Lắng nghe sự kiện qua RabbitMQ gửi Email hóa đơn tự động. Vận hành AI Chatbot tư vấn sản phẩm Google Gemini (chống Prompt Injection, Function Calling tra cứu sản phẩm), công cụ AI hỗ trợ Seller sinh bài mô tả chuẩn SEO và quản lý chiến dịch Flash Sale AI Auto-Approve. 
   *Cơ sở dữ liệu:* `notification_db`.

---

# **3. Yêu cầu Chức năng (Functional Requirements)** 

## **3.1 Phân hệ Xác thực & Người dùng (Identity Module)** 

- **FR-01: Đăng ký & Đăng nhập (Authentication):** 
  - **Đăng ký:** Họ tên, Email, Mật khẩu (tối thiểu 8 ký tự, có chữ hoa, chữ số, ký tự đặc biệt). Kiểm tra trùng lặp Email. Mật khẩu mã hóa BCrypt. Tài khoản tạo ở trạng thái `PENDING_VERIFICATION`, OTP 6 số lưu Redis (TTL = 5 phút), gửi mail kích hoạt qua RabbitMQ. 
  - **Đăng nhập:** Kiểm tra mật khẩu và trạng thái `ACTIVE`. Cấp Access Token (short-lived) và Refresh Token. Hệ thống hỗ trợ thiết lập Refresh Token vào HttpOnly Secure SameSite Cookie và trả Access Token qua AuthResponse.
- **FR-01.1: Đăng nhập xã hội Google OAuth 2.0:** 
  - Hỗ trợ người dùng đăng nhập hoặc đăng ký một chạm bằng tài khoản Google. Tự động liên kết email, tạo hồ sơ người dùng và cấp JWT token an toàn.
- **FR-02: Làm mới Token & Quên mật khẩu:** 
  - **Refresh Token:** Gateway verify token, kiểm tra tính hợp lệ trong Redis Blacklist. Cấp Access Token mới nếu hợp lệ. 
  - **Quên mật khẩu:** Nhận Email $\rightarrow$ Tạo token reset lưu trong Redis (TTL 15 phút) $\rightarrow$ Gửi link/OTP qua Mail $\rightarrow$ Người dùng nhập mật khẩu mới $\rightarrow$ Cập nhật Database. 
- **FR-02.1: Đăng ký & Quản lý Gian hàng (Seller Onboarding & Shop Profile):** 
  - Cho phép người dùng chuyển đổi tài khoản thành Người bán, cập nhật hồ sơ định danh KYC (CCCD/MST, Tên Shop, Logo, Kho lấy hàng, Tài khoản ngân hàng rút tiền). 
  - Bản ghi trạng thái `PENDING_APPROVAL`. Admin duyệt chuyển thành `ACTIVE`, gán thêm quyền `ROLE_SELLER`, tự động kích hoạt tạo Ví Seller bên Payment Service và mở quyền truy cập Seller Center.
- **FR-02.2: Phân hệ Nhân viên Giao Hàng (`ROLE_SHIPPER`):** 
  - Cung cấp trang đăng ký và đăng nhập dành riêng cho bưu tá giao vận. Gán quyền `ROLE_SHIPPER`, liên kết tài khoản với hệ thống tiếp nhận và cập nhật trạng thái đơn hàng.

---

## **3.2 Phân hệ Sản phẩm & Danh mục (Catalog Module)** 

- **FR-03: Tìm kiếm, Lọc & Caching Sản phẩm:** 
  - Khách hàng tìm kiếm sản phẩm đa gian hàng theo từ khóa, danh mục sàn, khoảng giá, đánh giá sao, và tìm kiếm riêng theo từng Shop. 
  - Tích hợp giao diện Flagship Bento Grid và Modal Dialog bộ lọc căn giữa màn hình. Kiểm tra Redis Cache với TTL = 10 phút. Tốc độ $< 10ms$ khi Cache Hit. 
- **FR-04: Quản lý Biến thể, Soạn thảo CKEditor 5 & Kiểm duyệt Sản phẩm:** 
  - Người bán đăng tải và quản lý sản phẩm thuộc Shop mình với nhiều biến thể SKU (Màu sắc, Size, Giá, Tồn kho thực tế và Tồn kho đang giữ thanh toán). 
  - Tích hợp bộ soạn thảo văn bản phong phú CKEditor 5, hỗ trợ chèn ảnh nội tuyến trực tiếp lên kho lưu trữ MinIO qua kênh HTTPS an toàn. 
  - Invalidate Cache liên quan trong Redis. Sản phẩm tạo ở trạng thái `PENDING_APPROVAL`, sau khi Admin duyệt chuyển sang `ACTIVE` và xuất hiện trên Storefront.
- **FR-04.1: Đánh giá & Nhận xét Sản phẩm (Reviews & Ratings):**
  - Khách hàng sau khi đơn hàng Sub-Order chuyển trạng thái `COMPLETED` có quyền đánh giá 1-5 sao và để lại nhận xét kèm hình ảnh cho sản phẩm.
- **FR-04.2: Thống kê Doanh thu theo SKU:**
  - Bảng báo cáo số liệu phân tích tài chính chuyên sâu cho Người bán, hiển thị doanh thu, số lượng bán và tồn kho theo từng mã biến thể SKU.

---

## **3.3 Phân hệ Giỏ hàng (Cart Module)** 

- **FR-05: Quản lý & Đồng bộ Giỏ hàng (Guest to User):** 
  - Gom nhóm mặt hàng theo từng Shop riêng biệt (`shop_id`), hỗ trợ tick chọn từng Shop hoặc chọn tất cả để tiến hành checkout độc lập chuẩn phong cách Shopee. 
  - **Guest Cart:** Lưu ở LocalStorage trình duyệt. 
  - **User Cart:** Lưu trong Redis Hash Key `cart:{user_id}`, field là `variant_id`, value là JSON chứa thông tin số lượng và shop. 
  - **Quy tắc giải quyết xung đột khi hợp nhất giỏ hàng (Cart Merge Conflict Resolution):** Khi Guest đăng nhập, tự động gộp giỏ hàng tạm vào Redis Cart, cộng dồn số lượng và kiểm tra giới hạn tồn kho khả dụng.

---

## **3.4 Phân hệ Đơn hàng, Thanh toán & Vận chuyển (Order, Payment & Shipping)** 

- **FR-06: Đặt hàng, Voucher Kép & Saga Orchestrator Workflow:** 
  - Buyer Checkout giỏ hàng đa Shop. Hệ thống tách tự động thành 1 Master Order và các Sub-Orders theo từng Shop. 
  - Order Service đóng vai trò **Saga Orchestrator**: 
    1. Tạo Master Order và Sub-Orders (`DRAFT`). Lưu Snapshot giá và thông tin sản phẩm (`product_name, variant_name, image_url`). 
    2. Gửi lệnh giữ kho (Reserve Stock có TTL 15 phút) sang Catalog Service. 
    3. Kiểm tra tính hợp lệ của Voucher Sàn và Voucher từng Shop từ bảng `vouchers`. 
    4. Nếu giữ kho và voucher thành công: Tạo giao dịch thanh toán (PayOS QR, VNPay URL hoặc COD). Chuyển trạng thái sang `PENDING_PAYMENT`.
    5. Nếu thất bại hoặc hết hạn 15 phút không thanh toán: Kích hoạt Compensating Transaction để hoàn lại kho đã giữ và hủy đơn hàng (`CANCELLED`). 
    6. **Cơ chế Dead Letter Queue (DLQ):** Toàn bộ các thông điệp giữ kho / bù trừ kho thất bại sau 3 lần retry sẽ tự động đẩy vào RabbitMQ Dead Letter Queue (`order.dlq`) để xử lý an toàn. 
- **FR-07: Cổng Thanh toán VNPay Sandbox:** 
  - Xử lý VNPay IPN Webhook với chữ ký bảo mật HMAC-SHA512 và xác nhận giao dịch tự động. 
- **FR-07.4: Cổng Thanh toán PayOS VietQR EMVCo:**
  - Tự động sinh mã VietQR chuẩn EMVCo động chứa số tiền và nội dung chuyển khoản theo mã đơn hàng; xử lý webhook thanh toán thành công và cung cấp nút bấm đối soát thanh toán thủ công.
- **FR-07.1: Quản lý Escrow, Double-Entry Ledger, Phí Hoa hồng & Rút tiền Seller:** 
  - Áp dụng mô hình Sổ cái kép (Double-Entry Ledger) chặt chẽ: Nợ tài khoản Escrow sàn, Có tài khoản Ví Seller. 
  - Tự động khấu trừ Phí hoa hồng sàn 5% và phí vận chuyển. 
  - **Điều kiện giải phóng Escrow:** Tự động giải phóng sau 72 giờ kể từ khi đơn hàng ở trạng thái `DELIVERED` mà không có khiếu nại, hoặc Buyer ấn xác nhận "Đã nhận được hàng". 
  - Seller tạo lệnh Rút tiền (`withdrawals`) $\rightarrow$ Admin duyệt $\rightarrow$ Chuyển khoản và hạch toán trừ số dư ví khả dụng. 
- **FR-07.5: Kênh Giao hàng Shipper Portal & Webhook Idempotency:**
  - Giao diện dành riêng cho bưu tá giao vận: Xem danh sách kiện hàng cần lấy (`READY_TO_PICK`), cập nhật đang giao (`DELIVERING`) và xác nhận đã giao (`DELIVERED`).
  - Tiếp nhận Webhook từ nhà vận chuyển với cơ chế Idempotency dựa trên `event_id` chống xử lý lặp sự kiện.
- **FR-07.2: Quản lý Đổi trả / Khiếu nại (Refund & Return Workflow):** 
  - Khách hàng có quyền yêu cầu Đổi trả/Hoàn tiền cho từng Sub-Order trong vòng 3 ngày kể từ khi giao hàng thành công (lưu vào bảng `refund_requests`). Đóng băng Escrow Sub-Order tương ứng độc lập. 
- **FR-07.3: Tác vụ Định kỳ Hệ thống (Scheduled Tasks với ShedLock):** 
  - Quét mỗi 5 phút các đơn `PENDING_PAYMENT` quá 15 phút để hủy và nhả kho giữ. 
  - Tự động chuyển tiền Escrow vào ví Seller cho các Sub-Order giao thành công quá 72 giờ không khiếu nại. 

---

## **3.5 Phân hệ Quản trị & Tiện ích Mở rộng (Admin & Advanced Features)** 

- **FR-08: Báo cáo Thống kê Doanh thu:** Tổng quan kinh doanh Marketplace, tổng GMV toàn sàn, tổng phí hoa hồng 5% thu được, dòng tiền ký quỹ Escrow và doanh thu theo từng Shop. 
- **FR-09: Quản lý Hệ thống, Audit Log & Phân quyền:** Quản lý tài khoản 4 vai trò, duyệt gian hàng, kiểm duyệt bài đăng sản phẩm, cấu hình danh mục sàn, xử lý khiếu nại hoàn tiền và lưu vết Audit Log.
- **FR-09.1: Bản đồ số Leaflet OpenStreetMap & Nominatim Geocoding:**
  - Tích hợp bản đồ số tương tác khi chọn địa chỉ giao nhận; chuyển đổi địa chỉ thành tọa độ GPS (Kinh độ/Vĩ độ) lưu vào hồ sơ địa chỉ; cấu hình Nginx Tile Cache & Geocoding Proxy chống Mixed Content.
- **FR-09.2: Quản trị Chiến dịch Flash Sale (Manual & AI Mode):**
  - Quản lý khung giờ giảm giá sốc với 2 chế độ: Chọn sản phẩm thủ công (Manual) hoặc kích hoạt Trí tuệ nhân tạo (AI Mode có công tắc Auto-Approve); đồng hồ đếm ngược đồng bộ toàn hệ thống.
- **FR-09.3: Hệ thống Tin nhắn Thời gian thực (Full-page Messenger):**
  - Giao diện chat toàn màn hình phong cách Facebook Messenger / Shopee Chat giữa Người mua và Người bán; tự động nhúng Thẻ sản phẩm thông minh đang xem vào khung trò chuyện.
- **FR-09.4: Trang Giới thiệu 3D Scrollytelling Flagship (IntroPage):**
  - Trình diễn 6 phân hệ công nghệ cao bằng WebGL/Three.js, cuộn mượt Lenis và chuyển động GSAP mượt mà.

---

## **3.6 Phân hệ Trợ lý AI (AI Module - Google Gemini API)** 

- **FR-10: AI Chatbot Tư vấn Mua sắm (An toàn & Chống Prompt Injection):** 
  - Chatbot tư vấn sản phẩm đa gian hàng theo thời gian thực tích hợp Google Gemini API. 
  - Làm sạch câu hỏi đầu vào (Prompt Sanitization) chống tấn công Prompt Injection. 
  - Gọi Gemini Function Calling truy vấn dữ liệu Catalog Service. Trang bị Circuit Breaker & Fallback trả kết quả tìm kiếm mặc định khi cạn Quota. 
- **FR-11: Sinh Mô tả Sản phẩm Chuẩn SEO:** Hỗ trợ Seller sinh bài mô tả chuẩn SEO dạng Markdown/HTML từ các thông số kỹ thuật ngắn gọn.

---

# **4. Yêu cầu Phi chức năng (Non-Functional Requirements)** 

## **4.1 Hiệu năng (Performance)** 
- **Response Time:** API đọc $t < 10ms$ (Cache Hit), $t < 200ms$ (Cache Miss). Phản hồi đăng nhập sai mật khẩu $< 200ms$. 
- **Throughput:** Đạt tối thiểu 1.000 RPS đỉnh điểm. 
- **Tối ưu Caching:** 90% lượt truy vấn danh sách sản phẩm được phục vụ trực tiếp từ Redis. 
- **Tối ưu RAM VPS:** Tối ưu hóa JVM heap/metaspace/Tomcat threads giúp toàn bộ cụm 7 microservices + hạ tầng vận hành ổn định trên VPS 1GB RAM.

## **4.2 Bảo mật (Security & QA Hardening)** 
- Vượt qua 18/18 tiêu chí kiểm thử bảo mật QA khắt khe:
  - Toàn bộ kết nối sử dụng HTTPS (TLS 1.3). JWT Tokens lưu trong HttpOnly Secure SameSite Cookies (hoặc Bearer Authorization Header) chống XSS. 
  - Kiểm tra tính chuẩn tắc chữ ký Base64URL canonical của JWT Tokens.
  - Rate Limiting thuật toán Token Bucket tại Gateway dựa theo header `CF-Connecting-IP`. 
  - Chống OWASP Top 10: SQL Injection (JPA PreparedStatement & SQLi fast-reject), XSS (DOMPurify), IDOR protection. 
  - Cấu hình Nginx Security Headers toàn diện: CSP, X-Frame-Options, HSTS, X-Content-Type-Options.
  - Hạ tầng mạng cô lập: Các Database (MySQL), Redis, RabbitMQ chỉ lắng nghe trong Docker Internal Network, tuyệt đối không public port ra ngoài Internet. 

## **4.3 Quy trình CI/CD & DevOps** 
- Quản lý mã nguồn theo Git Flow trên GitHub với 389 commits chi tiết.
- GitHub Actions tự động kích hoạt: Run Unit Test $\rightarrow$ Build JAR File $\rightarrow$ Build Docker Image $\rightarrow$ Deploy lên VPS. 
- Triển khai Zero-Downtime Deployment thông qua Docker Compose & Nginx Upstream Reload. 

## **4.4 Giám sát & Quản lý Log (Monitoring & Observability)** 
- **Prometheus & Grafana:** Thu thập số liệu tài nguyên (CPU, RAM, Request Rate, Error Rate 5xx) qua Spring Actuator và trực quan hóa dashboard. 
- **Micrometer & Zipkin:** Gắn Correlation ID (`X-Correlation-Id`) xuyên suốt qua các cuộc gọi HTTP và Message RabbitMQ để truy vết phân tán (Distributed Tracing).

---

# **5. Thiết kế Cơ sở dữ liệu Chuẩn (Database-per-Service Architecture)** 

Mỗi Microservice sở hữu một Database MySQL riêng biệt. Toàn bộ các giá trị tiền tệ được chuẩn hóa bằng kiểu `DECIMAL(12,0)` phù hợp cho Việt Nam Đồng (VND).

## **5.1 Identity DB (`identity_db`)** 
- **Bảng `users`:** `id` (PK), `username`, `email`, `password` (BCrypt), `full_name`, `phone_number`, `role` (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`, `ROLE_SHIPPER`), `status` (`PENDING_VERIFICATION`, `ACTIVE`, `BLOCKED`), `created_at`, `updated_at`.
- **Bảng `shops`:** `id` (PK), `owner_id` (Ref `users.id`), `shop_name`, `tax_code`, `phone_number`, `bank_code`, `bank_account_number`, `bank_account_name`, `status` (`PENDING_APPROVAL`, `ACTIVE`, `SUSPENDED`, `REJECTED`), `warehouse_address`, `created_at`.
- **Bảng `user_addresses`:** `id` (PK), `user_id`, `receiver_name`, `phone_number`, `detail_address`, `latitude` (DECIMAL(10,8)), `longitude` (DECIMAL(11,8)), `is_default`.

## **5.2 Catalog DB (`catalog_db`)** 
- **Bảng `categories`:** `id` (PK), `name`, `slug`, `icon_url`, `parent_id`.
- **Bảng `products`:** `id` (PK), `shop_id`, `category_id`, `name`, `description` (CKEditor 5 HTML), `main_image` (MinIO HTTPS URL), `status` (`PENDING_APPROVAL`, `ACTIVE`, `REJECTED`), `created_at`.
- **Bảng `product_variants`:** `id` (PK), `product_id` (FK `products.id`), `sku` (UNIQUE), `color`, `size`, `price`, `stock`, `reserved_stock`, `version` (Optimistic Locking).
- **Bảng `product_reviews`:** `id` (PK), `product_id`, `sub_order_id`, `user_id`, `rating` (1-5), `comment`, `created_at`.

## **5.3 Order DB (`order_db`)** 
- **Bảng `vouchers`:** `id` (PK), `code`, `shop_id` (NULL = Sàn), `discount_type`, `discount_value`, `min_order_value`, `max_discount_amount`, `usage_limit`, `used_count`, `start_date`, `end_date`, `status`.
- **Bảng `master_orders`:** `id` (PK), `user_id`, `total_amount`, `voucher_discount`, `shipping_fee_total`, `status` (`DRAFT`, `PENDING_PAYMENT`, `PROCESSING`, `COMPLETED`, `CANCELLED`, `REFUNDED`), `payment_method` (`COD`, `VNPAY`, `PAYOS_QR`), `shipping_address`, `created_at`.
- **Bảng `sub_orders`:** `id` (PK), `master_order_id` (FK), `shop_id`, `subtotal_amount`, `shipping_fee`, `shop_voucher_discount`, `final_amount`, `status` (`CREATED`, `CONFIRMED`, `PACKING`, `SHIPPING`, `DELIVERED`, `COMPLETED`, `CANCELLED`, `REFUNDED`), `created_at`.
- **Bảng `order_items`:** `id` (PK), `sub_order_id` (FK), `variant_id`, `product_name`, `variant_name`, `image_url`, `price`, `quantity`.
- **Bảng `refund_requests`:** `id` (PK), `sub_order_id` (UNIQUE), `user_id`, `reason`, `refund_amount`, `status` (`PENDING`, `ACCEPTED`, `REJECTED`), `admin_note`, `created_at`.

## **5.4 Payment DB (`payment_db`)** 
- **Bảng `wallets`:** `id` (PK), `shop_id` (UNIQUE), `balance`, `frozen_balance`.
- **Bảng `escrow_accounts`:** `id` (PK), `sub_order_id` (UNIQUE), `amount`, `commission_fee` (5%), `status` (`HOLDING`, `RELEASED`, `REFUNDED`), `auto_release_at`.
- **Bảng `ledger_entries`:** `id` (PK), `transaction_id`, `transaction_type`, `account_debit`, `account_credit`, `amount`, `created_at`.
- **Bảng `withdrawals`:** `id` (PK), `shop_id`, `amount`, `bank_code`, `bank_account_number`, `bank_account_name`, `status` (`PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`), `created_at`.

## **5.5 Shipping DB (`shipping_db`)** 
- **Bảng `shipments`:** `id` (PK), `sub_order_id` (UNIQUE), `tracking_code`, `carrier_name`, `shipping_fee`, `shipping_status` (`READY_TO_PICK`, `PICKED`, `DELIVERING`, `DELIVERED`, `DELIVERY_FAILED`, `RETURNED`), `shipper_id`, `updated_at`.
- **Bảng `shipping_webhooks_log`:** `id` (PK), `event_id` (UNIQUE - Idempotency), `carrier_name`, `payload`, `processed_at`.

## **5.6 Notification DB (`notification_db`)** 
- **Bảng `notifications`:** `id` (PK), `user_id`, `title`, `content`, `is_read`, `created_at`.
- **Bảng `email_logs`:** `id` (PK), `recipient_email`, `subject`, `status`, `sent_at`.
- **Bảng `flash_sales`:** `id` (PK), `campaign_name`, `start_time`, `end_time`, `is_ai_auto_approved`, `status`.
- **Bảng `flash_sale_items`:** `id` (PK), `flash_sale_id`, `product_id`, `discount_percentage`, `stock_limit`, `sold_count`.

---

# **6. Kế hoạch Phát triển & Phân công Công việc Chuẩn Agile/Scrum (4 Sprint)** 

Dự án được triển khai kỷ luật theo mô hình Agile/Scrum qua đúng **4 Sprint**, tương ứng 4 tuần lễ từ **26/10/2026 đến 20/11/2026**. Mỗi Sprint gồm đúng **5 ngày làm việc (Thứ Hai đến Thứ Sáu)**, hoàn toàn **không làm việc vào Thứ Bảy và Chủ Nhật**.

## **6.1 Phân cấp Tính năng theo MoSCoW (Hoàn thành 100%)** 

- **Must Have (Đã hoàn thành 100%):** 
  - 1 API Gateway + 6 Backend Services + Nginx Edge kết nối 6 DB MySQL độc lập.
  - Phân quyền RBAC 4 vai trò: `ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`, `ROLE_SHIPPER`.
  - Quản lý Gian hàng (Shop Onboarding & Phê duyệt KYC).
  - Catalog CRUD Sản phẩm, Biến thể SKU, upload ảnh MinIO S3, Redis Cache.
  - Giỏ hàng Redis gom nhóm theo Shop, tách Master Order $\rightarrow$ Sub-Orders theo Shop.
  - Saga Orchestrator kiểm tra giữ tồn kho có thời hạn (15 phút) qua RabbitMQ kèm cơ chế DLQ (`order.dlq`).
  - Thanh toán kép: VNPay Sandbox và PayOS VietQR EMVCo.
  - Quản lý Escrow tạm giữ & Ví Seller theo mô hình Sổ cái kép Double-Entry, tự động giải phóng tiền sau 72h.
  - Kênh Giao hàng Shipper Portal và Shipping Webhook Idempotency bằng `event_id`.
  - CI/CD GitHub Actions Rolling Update tự động lên VPS production.
- **Should Have (Đã hoàn thành 100%):** 
  - Bản đồ số Leaflet OpenStreetMap tích hợp Nominatim GPS Geocoding.
  - Đăng nhập xã hội Google OAuth 2.0 và xác thực OTP Email qua Redis TTL 5 phút.
  - Hệ thống Chat thời gian thực Full-page Messenger nhúng Thẻ sản phẩm thông minh.
  - Quản trị Flash Sale đa chế độ (Manual & AI Auto-Approve) kèm đồng hồ đếm ngược đồng bộ.
  - Trình soạn thảo văn bản phong phú CKEditor 5 chèn ảnh MinIO nội tuyến.
  - Áp dụng Voucher Kép (Sàn & Shop) và Đổi trả/Hoàn tiền trong 3 ngày.
  - Google Gemini AI Chatbot tư vấn sản phẩm + Tool sinh SEO description.
  - Giám sát Metrics với Prometheus/Grafana, Tracing với Zipkin.
- **Could Have (Đã hoàn thành xuất sắc):** 
  - Trang giới thiệu tương tác 3D Scrollytelling Flagship (WebGL/Three.js/GSAP/Lenis).
  - Bảng thống kê phân tích doanh thu chi tiết theo từng mã sản phẩm SKU cho Seller và Admin.
  - Vá toàn diện 18 tiêu chí an ninh QA và tối ưu RAM chạy mượt trên VPS 1GB.

---

## **6.2 Bảng Phân công Công việc Chi tiết 4 Sprint (Work Breakdown Structure - WBS)** 

| Sprint | Ngày làm việc | Thành viên | Phân hệ & Nội dung công việc chi tiết | Đầu ra bàn giao | Trạng thái |
|:---:|:---:|---|---|---|:---:|
| **S1** | **26/10/2026** (T2) | Lê Đức Anh | [Quản lý] Kick-off dự án; rà soát mục tiêu MVP, FR/NFR; lập backlog phạm vi | Biên bản kick-off, Backlog 4 Sprint | 100% |
| **S1** | **26/10/2026** (T2) | Triệu Quốc Bình | [Frontend] Khảo sát yêu cầu 4 phân hệ; sơ đồ luồng điều hướng UI/UX | User Journey Map, Wireframe UI | 100% |
| **S1** | **26/10/2026** (T2) | Nguyễn Văn Kiên | [Data & Order] Phân tích nghiệp vụ giỏ hàng đa shop, Checkout, Escrow | Tài liệu nghiệp vụ, State Machine | 100% |
| **S1** | **27/10/2026** (T3) | Lê Đức Anh | [Kiến trúc] Thiết kế sơ đồ 1 Gateway + 6 Backend Services, phân vùng mạng | Sơ đồ kiến trúc v1, Port Matrix | 100% |
| **S1** | **27/10/2026** (T3) | Triệu Quốc Bình | [Frontend] Khởi tạo dự án React 18, Vite, Tailwind CSS, Zustand routing | Frontend dev local, route khung | 100% |
| **S1** | **27/10/2026** (T3) | Nguyễn Văn Kiên | [Database] Thiết kế mô hình quan hệ ERD Order, Payment, Shipping, Notification | Bản vẽ ERD nhóm dịch vụ | 100% |
| **S1** | **28/10/2026** (T4) | Lê Đức Anh | [DevOps] Thiết lập Git conventions, Docker Compose cơ sở cho MySQL 8.0 | Git repo chuẩn, Compose MySQL | 100% |
| **S1** | **28/10/2026** (T4) | Triệu Quốc Bình | [Catalog] Thiết kế Schema Catalog DB: Category, Product, SKU, Review | Bản vẽ ERD, DDL Catalog DB v1 | 100% |
| **S1** | **28/10/2026** (T4) | Nguyễn Văn Kiên | [Order DB] Hoàn thiện DDL Schema Order DB: Master/Sub-Orders, VoucherUsage | DDL order_service_init.sql | 100% |
| **S1** | **29/10/2026** (T5) | Lê Đức Anh | [Hạ tầng] Tích hợp Redis 7, RabbitMQ 3.13 và MinIO S3 vào Docker Compose | Compose đủ Redis, MQ, MinIO | 100% |
| **S1** | **29/10/2026** (T5) | Triệu Quốc Bình | [Frontend] Dựng Storefront Layout: Header Shopee-style, Drawer giỏ hàng, Footer | Layout Storefront responsive | 100% |
| **S1** | **29/10/2026** (T5) | Nguyễn Văn Kiên | [Common Lib] Xây dựng thư viện common-library dùng chung cho microservices | common-library build thành công | 100% |
| **S1** | **30/10/2026** (T6) | Lê Đức Anh | [API Gateway] Khởi tạo Spring Cloud Gateway (8080), CORS, Correlation ID | Gateway Service chạy cổng 8080 | 100% |
| **S1** | **30/10/2026** (T6) | Triệu Quốc Bình | [Catalog] Khởi tạo Catalog Service (8082), kết nối DB, API Category CRUD | Catalog Service chạy cổng 8082 | 100% |
| **S1** | **30/10/2026** (T6) | Nguyễn Văn Kiên | [Order] Khởi tạo Order Service (8083), kết nối MySQL, Redis, Cart contract | Order Service chạy cổng 8083 | 100% |
| *--* | *31/10-01/11* | *Nghỉ T7-CN* | *Không làm việc* | *Nghỉ cuối tuần* | *--* |
| **S2** | **02/11/2026** (T2) | Lê Đức Anh | [Identity] Khởi tạo Identity Service (8081), API đăng ký, mã hóa BCrypt | Identity Service chạy cổng 8081 | 100% |
| **S2** | **02/11/2026** (T2) | Triệu Quốc Bình | [Catalog] API quản lý Sản phẩm theo Shop, vòng đời duyệt DRAFT/PENDING | API Product CRUD theo Shop | 100% |
| **S2** | **02/11/2026** (T2) | Nguyễn Văn Kiên | [Cart] Triển khai giỏ hàng phân tán Redis: Guest Cart và User Cart Hash | Redis Cart latency < 5ms | 100% |
| **S2** | **03/11/2026** (T3) | Lê Đức Anh | [Auth] Xác thực OTP qua Email với Redis TTL 5 phút, Rate-Limiting gửi mã | OTP flow + kiểm thử hết hạn | 100% |
| **S2** | **03/11/2026** (T3) | Triệu Quốc Bình | [Catalog] Quản lý biến thể SKU: Màu sắc, size, giá, tồn kho và khóa lạc quan | SKU Matrix API, chống âm kho | 100% |
| **S2** | **03/11/2026** (T3) | Nguyễn Văn Kiên | [Cart] Thuật toán tự động hợp nhất giỏ hàng (Guest -> User) khi đăng nhập | Cart merge logic không mất đồ | 100% |
| **S2** | **04/11/2026** (T4) | Lê Đức Anh | [Security] JWT Dual Auth (Cookie & Bearer), Refresh Token, Redis Blacklist | Auth endpoints + Gateway Filter | 100% |
| **S2** | **04/11/2026** (T4) | Triệu Quốc Bình | [Storage] Tích hợp MinIO S3 SDK upload ảnh sản phẩm, public stream URL | API upload ảnh MinIO chuẩn hóa | 100% |
| **S2** | **04/11/2026** (T4) | Nguyễn Văn Kiên | [Checkout] Tính toán Checkout đa shop, tách Sub-Orders, lưu snapshot giá | API Checkout tạo đơn đa shop | 100% |
| **S2** | **05/11/2026** (T5) | Lê Đức Anh | [Gateway] Cấu hình ma trận phân quyền RBAC BUYER, SELLER, ADMIN, lỗi 401/403 | Gateway RBAC Filter chuẩn hóa | 100% |
| **S2** | **05/11/2026** (T5) | Triệu Quốc Bình | [Catalog] Tích hợp Redis Caching cho Catalog Service, cơ chế Invalidate cache | Catalog API latency < 10ms | 100% |
| **S2** | **05/11/2026** (T5) | Nguyễn Văn Kiên | [Voucher] Engine tính toán Voucher 2 tầng: Áp dụng Voucher Sàn + Voucher Shop | Dual Voucher Engine chuẩn xác | 100% |
| **S2** | **06/11/2026** (T6) | Lê Đức Anh | [Shop KYC] Quy trình đăng ký gian hàng KYC hồ sơ Shop PENDING_APPROVAL | API đăng ký gian hàng KYC | 100% |
| **S2** | **06/11/2026** (T6) | Triệu Quốc Bình | [Frontend] Kết nối Buyer Catalog với API thật: Tìm kiếm, lọc giá, phân trang | Storefront duyệt hàng thật | 100% |
| **S2** | **06/11/2026** (T6) | Nguyễn Văn Kiên | [Shipping] Khởi tạo Shipping Service (8085), tính cước GHN/GHTK linh hoạt | Shipping Service chạy cổng 8085 | 100% |
| *--* | *07/11-08/11* | *Nghỉ T7-CN* | *Không làm việc* | *Nghỉ cuối tuần* | *--* |
| **S3** | **09/11/2026** (T2) | Lê Đức Anh | [Admin] Quy trình Admin duyệt gian hàng KYC, cấp ROLE_SELLER, bắn sự kiện | API duyệt Shop + audit log | 100% |
| **S3** | **09/11/2026** (T2) | Triệu Quốc Bình | [Seller UI] Xây dựng Kênh Người Bán (Seller Center): Dashboard & sản phẩm | Seller Center UI kết nối API | 100% |
| **S3** | **09/11/2026** (T2) | Nguyễn Văn Kiên | [Saga] Kiến trúc Saga Orchestration điều phối giao dịch phân tán qua RabbitMQ | Module Saga Orchestrator | 100% |
| **S3** | **10/11/2026** (T3) | Lê Đức Anh | [Notification] Khởi tạo Notification AI (8086), gửi email hóa đơn tự động | Notification Service chạy cổng 8086 | 100% |
| **S3** | **10/11/2026** (T3) | Triệu Quốc Bình | [Reviews] Phân hệ đánh giá 1-5 sao và tính điểm uy tín gian hàng sau COMPLETED | Review API & giao diện số sao | 100% |
| **S3** | **10/11/2026** (T3) | Nguyễn Văn Kiên | [Saga DLQ] Cơ chế bù trừ Saga Rollback và Dead Letter Queue (order.dlq) | Bồi hoàn Saga < 200ms, DLQ | 100% |
| **S3** | **11/11/2026** (T4) | Lê Đức Anh | [AI Core] Trợ lý ảo tư vấn mua sắm thông minh Google Gemini, chống Injection | AI Chatbot endpoint an toàn | 100% |
| **S3** | **11/11/2026** (T4) | Triệu Quốc Bình | [Admin UI] Xây dựng Cổng Quản Trị (Admin Portal): Kiểm duyệt sản phẩm & user | Admin Portal UI chuẩn responsive | 100% |
| **S3** | **11/11/2026** (T4) | Nguyễn Văn Kiên | [VNPay] Tích hợp cổng thanh toán VNPay Sandbox, xử lý VNPay IPN Idempotent | VNPay Sandbox thanh toán chuẩn | 100% |
| **S3** | **12/11/2026** (T5) | Lê Đức Anh | [AI Function] Gemini Function Calling tra cứu sản phẩm, Circuit Breaker quota | Chatbot AI tra cứu sản phẩm | 100% |
| **S3** | **12/11/2026** (T5) | Triệu Quốc Bình | [Frontend] Hoàn thiện trải nghiệm giỏ hàng Shopee-style và quy trình Checkout | Luồng giỏ hàng -> thanh toán mượt | 100% |
| **S3** | **12/11/2026** (T5) | Nguyễn Văn Kiên | [Ledger] Sổ cái kế toán kép (Double-entry Ledger) & Tài khoản ký quỹ Escrow | Module Sổ cái kép (Debit=Credit) | 100% |
| **S3** | **13/11/2026** (T6) | Lê Đức Anh | [AI SEO] Trí tuệ nhân tạo tự động viết mô tả sản phẩm chuẩn SEO cho Seller | Nút bấm "Tạo mô tả bằng AI" | 100% |
| **S3** | **13/11/2026** (T6) | Triệu Quốc Bình | [Admin Finance] Dashboard Tài chính Admin: GMV, hoa hồng 5%, duyệt rút tiền | Giao diện Quản trị Tài chính | 100% |
| **S3** | **13/11/2026** (T6) | Nguyễn Văn Kiên | [Shipping] Sinh mã vận đơn tự động, xử lý Webhook Idempotency theo event_id | Webhook Idempotent an toàn | 100% |
| *--* | *14/11-15/11* | *Nghỉ T7-CN* | *Không làm việc* | *Nghỉ cuối tuần* | *--* |
| **S4** | **16/11/2026** (T2) | Lê Đức Anh | [Motion] Nâng cấp giao diện Impeccable: Cuộn mượt Lenis và chuyển động GSAP | Trải nghiệm 60fps, Lenis scroll | 100% |
| **S4** | **16/11/2026** (T2) | Triệu Quốc Bình | [Redesign] Thiết kế Flagship: Bento Hero Grid, Floating Navbar, Modal lọc giữa | Giao diện Storefront Flagship | 100% |
| **S4** | **16/11/2026** (T2) | Nguyễn Văn Kiên | [Rich Text] Tích hợp CKEditor 5 và tải ảnh nội tuyến trực tiếp lên MinIO | Bộ soạn thảo mô tả phong phú | 100% |
| **S4** | **17/11/2026** (T3) | Lê Đức Anh | [3D Intro] Xây dựng trang tương tác 3D Scrollytelling 6 phân hệ (WebGL/Canvas) | Trang IntroPage 3D ấn tượng | 100% |
| **S4** | **17/11/2026** (T3) | Triệu Quốc Bình | [Messenger] Giao diện Chat Full-page Messenger Buyer - Seller nhúng thẻ sản phẩm | Hệ thống Chat trực tuyến | 100% |
| **S4** | **17/11/2026** (T3) | Nguyễn Văn Kiên | [PayOS VietQR] Tích hợp Cổng thanh toán chuyển khoản PayOS chuẩn VietQR EMVCo | Thanh toán VietQR quét mã tức thì | 100% |
| **S4** | **18/11/2026** (T4) | Lê Đức Anh | [Security QA] Vá toàn diện 18 lỗi bảo mật QA, chữ ký Base64URL JWT, Nginx CSP | 18/18 tiêu chí an ninh Passed | 100% |
| **S4** | **18/11/2026** (T4) | Triệu Quốc Bình | [Order UI] Hiển thị Sub-Orders tách biệt theo shop, Avatar và Modal vận trình | Giao diện đơn hàng minh bạch | 100% |
| **S4** | **18/11/2026** (T4) | Nguyễn Văn Kiên | [Escrow 72h] Tự động giải phóng tiền Escrow sau 72h và Bảng doanh thu theo SKU | Tự động hóa giải ngân ví Seller | 100% |
| **S4** | **19/11/2026** (T5) | Lê Đức Anh | [Shipper] Phân hệ Kênh Giao hàng (ROLE_SHIPPER): Mở rộng Identity & API bưu tá | Quyền bưu tá & API giao nhận | 100% |
| **S4** | **19/11/2026** (T5) | Triệu Quốc Bình | [Shipper UI] Xây dựng Cổng Shipper Portal tiếp nhận đơn, Thẻ sản phẩm Chatbot AI | Shipper Portal di động hoàn chỉnh | 100% |
| **S4** | **19/11/2026** (T5) | Nguyễn Văn Kiên | [Flash Sale] Cổng quản trị Flash Sale (Manual & AI Mode), đếm ngược đồng bộ | Flash Sale Manager sống động | 100% |
| **S4** | **20/11/2026** (T6) | Lê Đức Anh | [Google & Map] Đăng nhập Google OAuth, Bản đồ số Leaflet OSM, CI/CD Deploy | Google Auth, Map GPS, Production | 100% |
| **S4** | **20/11/2026** (T6) | Triệu Quốc Bình | [QA Frontend] Tổng duyệt kiểm thử E2E 4 phân hệ, tối ưu Responsive, User Guide | Giao diện 100% Passed, User Guide | 100% |
| **S4** | **20/11/2026** (T6) | Nguyễn Văn Kiên | [QA Backend] Tổng duyệt kiểm thử hồi quy Backend, kiểm tra Sổ cái, Bàn giao | Backend 100% Passed, Nghiệm thu | 100% |

---

## **6.3 Tổng hợp Trách nhiệm theo Thành viên** 

| Thành viên | Vai trò & Trách nhiệm chính | Chi tiết các hạng mục phụ trách chính |
|---|---|---|
| **Lê Đức Anh** | **Team Leader / System Architect / DevOps, Security & AI Lead** | • **Khởi tạo & Kiến trúc (Sprint 1):** Lập kế hoạch dự án 4 Sprint, rà soát FR/NFR, thiết kế kiến trúc Microservices 7 services.<br>• **Hạ tầng & API Gateway (Sprint 1, 2, 4):** Cấu hình Docker Compose (MySQL, Redis, RabbitMQ, MinIO), phát triển API Gateway (Routing, Dual Auth Cookie/Bearer, RBAC, Rate Limiting), CI/CD GitHub Actions & Nginx Reverse Proxy SSL, Nominatim Proxy.<br>• **Identity & Security Hardening (Sprint 2, 4):** Authentication API, Google OAuth 2.0, OTP Email Redis TTL 5 phút, Shop KYC Onboarding, Kênh Shipper `ROLE_SHIPPER`, vá 18 lỗi an ninh QA.<br>• **Trí tuệ nhân tạo & Showcase (Sprint 3, 4):** Google Gemini Chatbot tư vấn mua sắm, chống Prompt Injection, Tool AI sinh mô tả SEO, Trang giới thiệu tương tác 3D Scrollytelling Showcase (WebGL/GSAP).<br>• **Bàn giao (Sprint 4):** Điều phối nghiệm thu, cấu hình deploy production VPS và giám sát Prometheus/Grafana. |
| **Triệu Quốc Bình** | **Frontend Lead / UI-UX Specialist & Multi-Portal Integration** | • **Khởi tạo Frontend & Layout (Sprint 1):** Thiết lập React 18, Vite, Tailwind CSS, Zustand Store, layout Storefront Shopee-style.<br>• **Catalog Service & Storage (Sprint 1, 2):** Phân cấp danh mục, quản lý sản phẩm đa biến thể SKU, khóa lạc quan `@Version`, upload ảnh MinIO S3, Redis Cache invalidation.<br>• **Giao diện 4 Phân hệ (Sprint 2, 3, 4):** Phát triển hoàn chỉnh Storefront Flagship Bento Grid, Kênh Người Bán (Seller Center), Cổng Quản Trị (Admin Portal) và Kênh Nhân viên Giao Hàng (Shipper Portal).<br>• **Tính năng Mở rộng Cao cấp (Sprint 4):** Bản đồ số Leaflet OpenStreetMap chọn địa chỉ GPS, Hệ thống Tin nhắn Full-page Messenger nhúng Thẻ sản phẩm thông minh, Modal lọc trung tâm, hiệu ứng cuộn mượt Lenis và chuyển động GSAP.<br>• **QA & User Guide (Sprint 4):** Kiểm thử giao diện E2E đa thiết bị, hoàn thiện tài liệu Hướng dẫn sử dụng `USER_GUIDE.md` và bàn giao. |
| **Nguyễn Văn Kiên** | **Backend Lead / Saga Orchestrator / Transactions, Logistics & QA** | • **Thiết kế Dữ liệu & Common Lib (Sprint 1):** Thiết kế Schema CSDL chuẩn Database-per-Service cho 6 DBs và xây dựng thư viện dùng chung `common-library`.<br>• **Cart & Order Service (Sprint 1, 2):** Giỏ hàng phân tán Redis Hash, gộp giỏ hàng Guest-User, Checkout tách Master/Sub-Orders, Dual Voucher Engine (Sàn + Shop).<br>• **Saga Orchestrator & Transactions (Sprint 3):** Điều phối giao dịch phân tán qua RabbitMQ Exchange, Dead Letter Queue (`order.dlq`), bồi hoàn Rollback kho và voucher.<br>• **Thanh toán, Sổ cái & Vận chuyển (Sprint 2, 3, 4):** Cổng thanh toán kép VNPay Sandbox và PayOS VietQR EMVCo, Sổ cái kế toán kép (Double-entry Ledger), Ký quỹ Escrow tự động giải phóng sau 72h, tính cước GHN/GHTK, webhook idempotency theo `event_id`.<br>• **Flash Sale & Báo cáo Doanh thu (Sprint 4):** Cổng quản trị Flash Sale đa chế độ (Manual & AI Mode), Bảng thống kê doanh thu theo mã SKU, Tác vụ ShedLock hủy đơn quá hạn 15 phút.<br>• **QA & Nghiệm thu (Sprint 4):** Chủ trì kiểm thử hồi quy Backend (> 140 test cases 100% Passed) và ký kết Biên bản bàn giao kỹ thuật. |

---

*Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) được hoàn thiện và phê duyệt nghiệm thu ngày 20/11/2026 bởi Ban Kỹ thuật Dự án QuickShop.*
