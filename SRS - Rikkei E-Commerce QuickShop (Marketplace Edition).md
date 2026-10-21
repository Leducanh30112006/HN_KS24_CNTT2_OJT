# **ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS) - RIKKEI E-COMMERCE QUICKSHOP (MARKETPLACE EDITION)** 

**Sàn Thương mại Điện tử Đa người bán (Multi-Vendor Marketplace) - Microservices & DevOps Production-Ready** 

# **0. Lịch sử thay đổi tài liệu (Document Revision History)** 

| Phiên bản | Ngày | Tác giả | Tóm tắt thay đổi |
|---|---|---|---|
| 1.0.0 | 15/10/2026 | Lê Đức Anh | Khởi tạo tài liệu đặc tả ban đầu. |
| 2.0.0 | 21/10/2026 | Lê Đức Anh | Nâng cấp kiến trúc Microservices & chuẩn hóa DevOps. |
| 2.1.0 | 21/10/2026 | Lê Đức Anh | Bổ sung State Machine và thiết kế luồng Saga Orchestrator. |
| **2.2.0** | **21/10/2026** | **Lê Đức Anh** | **Chuẩn hóa toàn diện:** Thống nhất mô hình 7 services (1 Gateway + 6 Backend), chuẩn hóa cổng dịch vụ (Shipping 8085, Notification 8086), bổ sung các bảng CSDL còn thiếu (`vouchers`, `product_reviews`, `refund_requests`, `withdrawals`), hoàn thiện State Machine (thêm trạng thái giao thất bại/hoàn hàng, hủy đơn), tích hợp cơ chế Dual Auth (HttpOnly Cookie & Bearer Header) và phân định ranh giới Core Production vs Mock Simulators. |

---

# **1. Giới thiệu** 

## **1.1 Mục đích** 
Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này xác định toàn bộ các yêu cầu chức năng, phi chức năng, kiến trúc kỹ thuật và phương án triển khai cho sàn thương mại điện tử đa người bán (Multi-vendor Marketplace) **Rikkei E-Commerce QuickShop** theo mô hình hoạt động tương tự Shopee. Hệ thống được thiết kế theo chuẩn mã nguồn mở cấp độ Production, đảm bảo khả năng chịu tải cao, mở rộng linh hoạt theo kiến trúc Microservices (Database-per-Service) và tự động hóa toàn diện quy trình vận hành qua DevOps/CI-CD.

## **1.2 Phạm vi sản phẩm & Chuẩn hóa Thuật ngữ** 
Hệ thống cung cấp nền tảng kết nối toàn diện cho 4 nhóm đối tượng chính:

- **Khách hàng (Buyer):** Tìm kiếm/lọc sản phẩm đa gian hàng, quản lý giỏ hàng tách theo Shop, áp dụng Voucher kép (Sàn & Shop), thanh toán trực tuyến qua cổng VNPay/Ví điện tử/COD, theo dõi vận chuyển đơn hàng thời gian thực, tương tác AI Chatbot tư vấn, đánh giá/nhận xét gian hàng và yêu cầu Đổi trả/Hoàn tiền (Refund/Return). 
- **Người bán (Seller):** Đăng ký và quản lý hồ sơ Shop, quản lý danh mục & sản phẩm đa biến thể SKU, tiếp nhận và xử lý luồng đơn hàng con (Sub-Orders), kết nối đơn vị vận chuyển (GHN/GHTK), quản lý Ví Seller/Sổ cái dòng tiền kép (Double-Entry Ledger), đối soát tài chính và rút tiền về tài khoản ngân hàng, tạo chiến dịch khuyến mãi (Voucher Shop/Flash Sale) và sinh nội dung SEO tự động bằng AI. 
- **Quản trị viên (Admin):** Duyệt/Khóa hồ sơ mở Shop, kiểm duyệt sản phẩm mới, quản lý cây danh mục chung, điều phối và phân xử tranh chấp/khiếu nại hoàn tiền, cấu hình mức phí hoa hồng (Commission Fee), quản lý Voucher toàn sàn, cấu hình Banner/Trang chủ, tra cứu Audit Log và theo dõi báo cáo doanh thu/dòng tiền Marketplace. 
- **Vận hành (Ops/System):** Tích hợp hạ tầng lưu trữ Object Storage (MinIO S3), xác thực an toàn chuẩn JWT (hỗ trợ cả HttpOnly Secure Cookie lẫn Bearer Authorization Header) / RBAC, quản lý webhook idempotency kết nối đối tác vận chuyển, chạy các tác vụ định kỳ ShedLock, tự động hóa CI/CD triển khai Zero-Downtime và giám sát Log/Metrics/Tracing tập trung (Prometheus, Grafana, Loki, Zipkin).

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

---

## **1.3 Công nghệ sử dụng** 

| Tầng kiến trúc | Công nghệ áp dụng | Vai trò & Chi tiết triển khai |
|---|---|---|
| **Frontend** | ReactJS 18, Zustand, Tailwind CSS, Lucide Icons | SPA Storefront + Seller Center + Admin Dashboard, quản lý Client State tối ưu, UI Responsive |
| **Backend API** | Spring Boot 3.3.x, Java 17 | Xây dựng các Microservices độc lập, ShedLock cho Scheduled Tasks |
| **Microservices Framework** | Spring Cloud Gateway, OpenFeign, Resilience4j | API Routing, Rate Limiting (Token Bucket), Circuit Breaker, Inter-service Communication |
| **Security & AI** | Spring Security 6, JWT (HttpOnly Cookie & Bearer Header), Spring AI | Phân quyền RBAC (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`), chống Prompt Injection, Fallback AI |
| **Database** | MySQL 8.0 (Database per Service) | 6 Database độc lập cho 6 backend business services |
| **Caching & Messaging** | Redis 7, RabbitMQ 3.13 | Caching giỏ hàng/sản phẩm, Distributed Lock, Async Messaging (Saga Orchestrator Pattern, DLQ `order.dlq`) |
| **Storage & Static** | MinIO (S3 Compatible) | Object Storage cho hình ảnh sản phẩm, media gian hàng |
| **DevOps & Infra** | Docker, Nginx, GitHub Actions, Cloudflare | Containerization, Reverse Proxy, CI/CD Rolling Deployment, SSL & DDoS Protection |
| **Monitoring & Tracing** | Prometheus, Grafana, Loki, Micrometer/Zipkin | Giám sát chỉ số tài nguyên, log tập trung và Distributed Tracing (Correlation ID) |

---

# **2. Kiến trúc Hệ thống (Microservices Architecture)** 

Hệ thống Marketplace được thiết kế theo mô hình Microservices phân tán với **1 API Gateway và 6 Dịch vụ Backend cốt lõi**, tách biệt hoàn toàn cơ sở dữ liệu (Database-per-Service):

1. **API Gateway (Port 8080):** 
   Cổng giao tiếp duy nhất cho Storefront, Seller Center và Admin Portal. Đảm nhận API Routing, kiểm tra JWT Token (hỗ trợ trích xuất cả từ HttpOnly Cookie lẫn Bearer Header), phân quyền RBAC (`BUYER`, `SELLER`, `ADMIN`), áp dụng Rate Limiting chống DDoS dựa trên header `CF-Connecting-IP` (fallback `X-Forwarded-For`).
2. **Identity & Shop Service (Port 8081):** 
   Quản lý tài khoản đa vai trò, quy trình KYC/Đăng ký Gian hàng, phê duyệt hồ sơ Seller, phân quyền RBAC và quản lý thông tin Profile Shop. 
   *Cơ sở dữ liệu:* `identity_db`.
3. **Catalog & Product Service (Port 8082):** 
   Quản lý cây danh mục chung, đăng bán sản phẩm thuộc gian hàng, kiểm duyệt bài đăng, biến thể SKU, quản lý tồn kho và upload media lên MinIO S3 Server. Đánh giá nhận xét sản phẩm (`product_reviews`). 
   *Cơ sở dữ liệu:* `catalog_db`.
4. **Order & Cart Service (Port 8083):** 
   Quản lý giỏ hàng đa Shop trên Redis, tính toán giá trị đơn, áp dụng Voucher kép (Sàn + Shop), tách đơn hàng tự động thành 1 Master Order và các Sub-Orders theo từng Shop. Đóng vai trò **Saga Orchestrator** điều phối toàn bộ chuỗi giao dịch phân tán qua RabbitMQ kèm cơ chế Dead Letter Queue (`order.dlq`). Quản lý yêu cầu đổi trả/hoàn tiền (`refund_requests`). 
   *Cơ sở dữ liệu:* `order_db`.
5. **Payment & Wallet Service (Port 8084):** 
   Tích hợp cổng VNPay Sandbox (IPN & Return), quản lý Tài khoản Tạm giữ (Escrow) của Sàn cho cả thanh toán VNPay và COD, duy trì Sổ cái kép (Double-Entry Ledger) cho Ví Seller, tự động khấu trừ hoa hồng sàn (Commission Fee), quản lý các lệnh rút tiền (`withdrawals`). 
   *Cơ sở dữ liệu:* `payment_db`.
6. **Shipping & Logistics Service (Port 8085):** 
   Tích hợp API đơn vị vận chuyển (GHN/GHTK hoặc Mock Logistics Simulator), tính phí ship chuẩn xác theo địa chỉ, sinh mã vận đơn và tiếp nhận Idempotent Webhook cập nhật hành trình đơn hàng dựa trên `event_id`. 
   *Cơ sở dữ liệu:* `shipping_db`.
7. **Notification & AI Service (Port 8086):** 
   Lắng nghe sự kiện qua RabbitMQ gửi Email OTP/Thông báo đơn hàng. Vận hành AI Chatbot tư vấn sản phẩm (chống Prompt Injection) và công cụ AI hỗ trợ Seller sinh bài mô tả chuẩn SEO. 
   *Cơ sở dữ liệu:* `notification_db`.

---

# **3. Yêu cầu Chức năng (Functional Requirements)** 

## **3.1 Phân hệ Xác thực & Người dùng (Identity Module)** 

- **FR-01: Đăng ký & Đăng nhập (Authentication):** 
  - **Đăng ký:** Họ tên, Email, Mật khẩu (tối thiểu 8 ký tự, có chữ hoa, chữ số, ký tự đặc biệt). Kiểm tra trùng lặp Email. Mật khẩu mã hóa BCrypt. Tài khoản tạo ở trạng thái `PENDING_VERIFICATION`, OTP 6 số lưu Redis (TTL = 5 phút), gửi mail kích hoạt qua RabbitMQ. 
  - **Đăng nhập:** Kiểm tra mật khẩu và trạng thái `ACTIVE`. Cấp Access Token (short-lived) và Refresh Token. Hệ thống hỗ trợ thiết lập Refresh Token vào HttpOnly Secure SameSite Cookie và trả Access Token qua AuthResponse.
- **FR-02: Làm mới Token & Quên mật khẩu:** 
  - **Refresh Token:** Gateway verify token, kiểm tra tính hợp lệ trong Redis Blacklist. Cấp Access Token mới nếu hợp lệ. 
  - **Quên mật khẩu:** Nhận Email $\rightarrow$ Tạo token reset lưu trong Redis (TTL 15 phút) $\rightarrow$ Gửi link/OTP qua Mail $\rightarrow$ Người dùng nhập mật khẩu mới $\rightarrow$ Cập nhật Database. 
- **FR-02.1: Đăng ký & Quản lý Gian hàng (Seller Onboarding & Shop Profile):** 
  - Cho phép người dùng chuyển đổi tài khoản thành Người bán, cập nhật hồ sơ định danh KYC (CCCD/MST, Tên Shop, Logo, Kho lấy hàng, Tài khoản ngân hàng rút tiền). 
  - Bản ghi trạng thái `PENDING_APPROVAL`. Admin duyệt chuyển thành `ACTIVE`, gán thêm quyền `ROLE_SELLER`, tự động kích hoạt tạo Ví Seller bên Payment Service và mở quyền truy cập Seller Center.

---

## **3.2 Phân hệ Sản phẩm & Danh mục (Catalog Module)** 

- **FR-03: Tìm kiếm, Lọc & Caching Sản phẩm:** 
  - Khách hàng tìm kiếm sản phẩm đa gian hàng theo từ khóa, danh mục sàn, khoảng giá, đánh giá sao, và tìm kiếm riêng theo từng Shop. 
  - Kiểm tra Redis Cache. Nếu Cache Miss: truy vấn Catalog DB có Index, ghi lại vào Redis với TTL = 10 phút. Phân trang chuẩn RESTful. Tốc độ $< 100ms$ khi Cache Hit. 
- **FR-04: Quản lý Biến thể, Upload Ảnh MinIO & Kiểm duyệt Sản phẩm:** 
  - Người bán đăng tải và quản lý sản phẩm thuộc Shop mình với nhiều biến thể SKU (Màu sắc, Size, Giá, Tồn kho thực tế và Tồn kho đang giữ thanh toán). 
  - Upload file media lên MinIO S3 Server qua SDK, nhận public URL. Lưu thông tin vào `products` và `product_variants`. 
  - Invalidate Cache liên quan trong Redis. Sản phẩm tạo ở trạng thái `PENDING_APPROVAL`, sau khi Admin duyệt chuyển sang `ACTIVE` và xuất hiện trên Storefront.
- **FR-04.1: Đánh giá & Nhận xét Sản phẩm (Reviews & Ratings):**
  - Khách hàng sau khi đơn hàng Sub-Order chuyển trạng thái `COMPLETED` có quyền đánh giá 1-5 sao và để lại nhận xét kèm hình ảnh cho sản phẩm.

---

## **3.3 Phân hệ Giỏ hàng (Cart Module)** 

- **FR-05: Quản lý & Đồng bộ Giỏ hàng (Guest to User):** 
  - Gom nhóm mặt hàng theo từng Shop riêng biệt (`shop_id`), hỗ trợ tick chọn từng Shop để tiến hành checkout độc lập. 
  - **Guest Cart:** Lưu ở LocalStorage trình duyệt. 
  - **User Cart:** Lưu trong Redis Hash Key `cart:{user_id}`, field là `variant_id`, value là JSON chứa thông tin số lượng và shop. 
  - **Quy tắc giải quyết xung đột khi hợp nhất giỏ hàng (Cart Merge Conflict Resolution):** Khi Guest đăng nhập, hệ thống duyệt từng mặt hàng trong Guest Cart: 
    - Nếu mặt hàng (SKU) chưa có trong giỏ hàng User: Thêm mới toàn bộ số lượng vào Redis Cart. 
    - Nếu mặt hàng (SKU) đã tồn tại trong giỏ hàng User: Cộng dồn số lượng ($qty_{user} = qty_{user} + qty_{guest}$). Nếu vượt quá tồn kho khả dụng hiện tại, điều chỉnh về mức tồn kho tối đa khả dụng và kèm thông báo cho khách hàng.

---

## **3.4 Phân hệ Đơn hàng & Thanh toán (Order & Payment Module)** 

- **FR-06: Đặt hàng, Voucher Kép & Saga Orchestrator Workflow:** 
  - Buyer Checkout giỏ hàng đa Shop. Hệ thống tách tự động thành 1 Master Order và các Sub-Orders theo từng Shop. 
  - Order Service đóng vai trò **Saga Orchestrator**: 
    1. Tạo Master Order và Sub-Orders (`DRAFT`). Lưu Snapshot giá và thông tin sản phẩm (`product_name, variant_name, image_url`). 
    2. Gửi lệnh giữ kho (Reserve Stock có TTL 15 phút) sang Catalog Service. 
    3. Kiểm tra tính hợp lệ của Voucher Sàn và Voucher từng Shop từ bảng `vouchers`. 
    4. Nếu giữ kho và voucher thành công: Tạo giao dịch thanh toán (VNPay URL hoặc xác nhận COD). Chuyển trạng thái sang `PENDING_PAYMENT`.
    5. Nếu thất bại hoặc hết hạn 15 phút không thanh toán: Kích hoạt Compensating Transaction để hoàn lại kho đã giữ và hủy đơn hàng (`CANCELLED`). 
    6. **Cơ chế Dead Letter Queue (DLQ):** Toàn bộ các thông điệp giữ kho / bù trừ kho thất bại sau 3 lần retry sẽ tự động đẩy vào RabbitMQ Dead Letter Queue (`order.dlq`) để kích hoạt cảnh báo cho kỹ sư vận hành xử lý thủ công. 
- **FR-07: Cổng Thanh toán VNPay & Idempotent Shipping Webhook:** 
  - Xử lý VNPay IPN Webhook với chữ ký bảo mật HMAC-SHA512. 
  - Đơn COD: Số tiền thu hộ được đối soát định kỳ và ghi nhận vào tài khoản COD Escrow. 
  - Tiếp nhận Webhook từ GHN/GHTK: Áp dụng cơ chế Idempotency qua `event_id` để tránh xử lý lặp trạng thái vận đơn. 
- **FR-07.1: Quản lý Escrow, Double-Entry Ledger, Phí Hoa hồng & Rút tiền Seller:** 
  - Áp dụng mô hình Sổ cái kép (Double-Entry Ledger) chặt chẽ: Nợ tài khoản Escrow sàn, Có tài khoản Ví Seller. 
  - Tự động khấu trừ Phí hoa hồng sàn (ví dụ: 3% - cấu hình linh hoạt bởi Admin) và phí vận chuyển. 
  - **Điều kiện giải phóng Escrow:** Sau 3 ngày kể từ khi đơn hàng ở trạng thái `DELIVERED` mà không có khiếu nại, hoặc Buyer ấn xác nhận "Đã nhận được hàng". 
  - Seller tạo lệnh Rút tiền (`withdrawals`) $\rightarrow$ Admin duyệt $\rightarrow$ Chuyển khoản và hạch toán trừ số dư ví khả dụng. 
- **FR-07.2: Quản lý Đổi trả / Khiếu nại (Refund & Return Workflow):** 
  - Khách hàng có quyền yêu cầu Đổi trả/Hoàn tiền cho từng Sub-Order trong vòng 3 ngày kể từ khi giao hàng thành công (lưu vào bảng `refund_requests`). 
  - Đóng băng quá trình giải phóng tiền Escrow của Sub-Order đó. Hỗ trợ Hoàn tiền một phần (Partial Refund) độc lập, không làm gián đoạn các Sub-Orders khác trong cùng Master Order. 
- **FR-07.3: Các Tác vụ Định kỳ Hệ thống (Scheduled Tasks với ShedLock):** 
  - Tự động Hủy đơn chưa thanh toán: Quét mỗi 5 phút các đơn `PENDING_PAYMENT` quá 15 phút để nhả kho giữ. 
  - Tự động Giải phóng Escrow: Chạy lúc 00:00 hàng ngày, chuyển tiền vào ví Seller cho các Sub-Order giao thành công quá 3 ngày không khiếu nại. 
  - Toàn bộ Scheduled Tasks được đồng bộ bằng Redis ShedLock chống chạy trùng giữa các container instance.

---

## **3.5 Phân hệ Quản trị (Admin Module)** 
- **FR-08: Báo cáo Thống kê Doanh thu:** Tổng quan kinh doanh Marketplace, tổng GMV toàn sàn, tổng phí hoa hồng thu được và doanh thu theo từng Shop. 
- **FR-09: Quản lý Hệ thống, Audit Log, Banner & Phân xử Tranh chấp:** Quản lý tài khoản, duyệt gian hàng, kiểm duyệt bài đăng sản phẩm, cấu hình danh mục sàn, xử lý khiếu nại hoàn tiền và lưu vết Audit Log.

---

## **3.6 Phân hệ Trợ lý AI (AI Module - Spring AI)** 
- **FR-10: AI Chatbot Tư vấn Mua sắm (An toàn & Chống Prompt Injection):** 
  - Chatbot tư vấn sản phẩm đa gian hàng theo thời gian thực. 
  - Làm sạch câu hỏi đầu vào (Prompt Sanitization) chống tấn công Prompt Injection. 
  - Gọi Spring AI Function Calling truy vấn Catalog Service. Cơ chế Circuit Breaker & Fallback trả kết quả tìm kiếm mặc định khi LLM timeout. 
- **FR-11: Sinh Mô tả Sản phẩm Chuẩn SEO:** Hỗ trợ Seller sinh bài mô tả chuẩn SEO dạng Markdown/HTML từ các thông số kỹ thuật ngắn gọn.

---

# **4. Yêu cầu Phi chức năng (Non-Functional Requirements)** 

## **4.1 Hiệu năng (Performance)** 
- **Response Time:** API đọc $t < 100ms$ (Cache Hit), $t < 300ms$ (Cache Miss). API ghi dữ liệu $t < 500ms$. 
- **Throughput:** Đạt tối thiểu 1.000 RPS đỉnh điểm. 
- **Tối ưu Caching:** 90% lượt truy vấn danh sách sản phẩm được phục vụ trực tiếp từ Redis. 

## **4.2 Bảo mật (Security)** 
- Toàn bộ kết nối sử dụng HTTPS (TLS 1.3). JWT Tokens lưu trong HttpOnly Secure SameSite Cookies (hoặc Bearer Authorization Header) chống tấn công XSS. 
- Rate Limiting thuật toán Token Bucket tại Gateway dựa theo header `CF-Connecting-IP`. 
- Chống OWASP Top 10: SQL Injection (JPA PreparedStatement), XSS, CSRF. 
- Hạ tầng mạng cô lập: Các Database (MySQL), Redis, RabbitMQ chỉ lắng nghe trong Docker Internal Network, tuyệt đối không public port ra ngoài Internet. 

## **4.3 Quy trình CI/CD & DevOps** 
- Developer push code lên nhánh main trên GitHub. 
- GitHub Actions tự động kích hoạt: Run Unit Test $\rightarrow$ Build JAR File $\rightarrow$ Build Docker Image $\rightarrow$ Push Image lên Docker Hub. 
- Triển khai Zero-Downtime Deployment bằng kỹ thuật Rolling Update / Blue-Green thông qua Docker Compose & Nginx Upstream Reload. 

## **4.4 Giám sát & Quản lý Log (Monitoring & Observability)** 
- **Loki Driver:** Thu thập toàn bộ log stdout/stderr của Docker Container. 
- **Prometheus & Grafana:** Thu thập số liệu tài nguyên (CPU, RAM, Request Rate, Error Rate 5xx) và gửi cảnh báo Telegram/Slack. 
- **Micrometer & Zipkin:** Gắn Correlation ID (`X-Correlation-Id`) xuyên suốt qua các cuộc gọi HTTP và Message RabbitMQ để truy vết phân tán (Distributed Tracing).

---

# **5. Thiết kế Cơ sở dữ liệu Chuẩn (Database-per-Service Architecture)** 

Mỗi Microservice sở hữu một Database MySQL riêng biệt. Toàn bộ các giá trị tiền tệ được chuẩn hóa bằng kiểu `DECIMAL(12,0)` phù hợp cho Việt Nam Đồng (VND).

## **5.1 Identity DB (`identity_db`)** 

- **Bảng `users`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID người dùng 
  - `username` (VARCHAR(50), UNIQUE, NOT NULL): Tên đăng nhập
  - `email` (VARCHAR(100), UNIQUE, NOT NULL): Email đăng nhập 
  - `password` (VARCHAR(255), NOT NULL): Mật khẩu mã hóa BCrypt 
  - `full_name` (VARCHAR(100), NOT NULL): Họ và tên 
  - `phone_number` (VARCHAR(20), NULL): Số điện thoại 
  - `role` (VARCHAR(30), NOT NULL, DEFAULT 'ROLE_BUYER'): ROLE_BUYER, ROLE_SELLER, ROLE_ADMIN
  - `status` (VARCHAR(30), NOT NULL, DEFAULT 'PENDING_VERIFICATION'): PENDING_VERIFICATION, ACTIVE, BLOCKED 
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 
  - `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)

- **Bảng `shops`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Gian hàng 
  - `owner_id` (BIGINT, NOT NULL, UNIQUE): Ref tới `users.id` 
  - `shop_name` (VARCHAR(100), NOT NULL, UNIQUE): Tên gian hàng 
  - `tax_code` (VARCHAR(50), NULL): Mã số thuế / CCCD
  - `phone_number` (VARCHAR(20), NULL): Hotline gian hàng
  - `bank_code` (VARCHAR(20), NULL): Ngân hàng nhận tiền (VCB, MB, TCB...)
  - `bank_account_number` (VARCHAR(50), NULL): Số tài khoản ngân hàng
  - `bank_account_name` (VARCHAR(100), NULL): Tên chủ tài khoản
  - `status` (VARCHAR(30), NOT NULL, DEFAULT 'PENDING_APPROVAL'): PENDING_APPROVAL, ACTIVE, SUSPENDED, REJECTED 
  - `warehouse_address` (TEXT, NOT NULL): Địa chỉ kho lấy hàng 
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 

---

## **5.2 Catalog DB (`catalog_db`)** 

- **Bảng `categories`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Danh mục 
  - `name` (VARCHAR(100), NOT NULL): Tên danh mục 
  - `slug` (VARCHAR(100), UNIQUE, NOT NULL): Đường dẫn SEO 

- **Bảng `products`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Sản phẩm 
  - `shop_id` (BIGINT, NOT NULL): Logical FK tới `shops.id` 
  - `category_id` (BIGINT, NOT NULL): Logical FK tới `categories.id` 
  - `name` (VARCHAR(255), NOT NULL): Tên sản phẩm 
  - `description` (MEDIUMTEXT, NULL): Mô tả chi tiết HTML/Markdown (chứa tối đa 16MB) 
  - `main_image` (VARCHAR(500), NULL): URL ảnh trên MinIO 
  - `status` (VARCHAR(30), NOT NULL, DEFAULT 'PENDING_APPROVAL'): PENDING_APPROVAL, ACTIVE, REJECTED 
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 

- **Bảng `product_variants`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Biến thể 
  - `product_id` (BIGINT, NOT NULL, FK `products.id`): Thuộc sản phẩm nào 
  - `sku` (VARCHAR(50), UNIQUE, NOT NULL): Mã SKU kho hàng 
  - `color` (VARCHAR(50), NULL): Màu sắc 
  - `size` (VARCHAR(20), NULL): Kích cỡ 
  - `price` (DECIMAL(12,0), NOT NULL): Giá bán VND 
  - `stock` (INT, NOT NULL, DEFAULT 0): Tồn kho thực tế 
  - `reserved_stock` (INT, NOT NULL, DEFAULT 0): Tồn kho đang giữ thanh toán 

- **Bảng `product_reviews`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Đánh giá
  - `product_id` (BIGINT, NOT NULL, FK `products.id`): Đánh giá sản phẩm nào
  - `sub_order_id` (BIGINT, NOT NULL): Gắn với Sub-Order cụ thể
  - `user_id` (BIGINT, NOT NULL): Người đánh giá
  - `rating` (TINYINT, NOT NULL CHECK (`rating` BETWEEN 1 AND 5)): Số sao đánh giá (1-5)
  - `comment` (TEXT, NULL): Nội dung nhận xét
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

---

## **5.3 Order DB (`order_db`)** 

- **Bảng `vouchers`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Voucher
  - `code` (VARCHAR(50), UNIQUE, NOT NULL): Mã voucher (VD: QUICKSHOP10, SHOPVIP20)
  - `shop_id` (BIGINT, NULL): NULL nếu là Voucher toàn sàn, có ID nếu là Voucher của Shop
  - `discount_type` (VARCHAR(20), NOT NULL): PERCENTAGE, FIXED_AMOUNT
  - `discount_value` (DECIMAL(12,0), NOT NULL): Giá trị giảm (% hoặc số tiền)
  - `min_order_value` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Giá trị đơn hàng tối thiểu
  - `max_discount_amount` (DECIMAL(12,0), NULL): Mức giảm tối đa với Voucher %
  - `usage_limit` (INT, NOT NULL): Tổng lượt dùng tối đa
  - `used_count` (INT, NOT NULL, DEFAULT 0): Số lượt đã dùng
  - `start_date` (TIMESTAMP, NOT NULL): Ngày bắt đầu
  - `end_date` (TIMESTAMP, NOT NULL): Ngày hết hạn
  - `status` (VARCHAR(20), NOT NULL, DEFAULT 'ACTIVE'): ACTIVE, INACTIVE, EXPIRED

- **Bảng `master_orders` (Đơn hàng Tổng):** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Master Order 
  - `user_id` (BIGINT, NOT NULL): Logical FK tới `users.id` 
  - `total_amount` (DECIMAL(12,0), NOT NULL): Tổng thanh toán toàn đơn (VND) 
  - `voucher_discount` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Giảm giá từ Voucher sàn 
  - `shipping_fee_total` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Tổng phí vận chuyển các shop
  - `status` (VARCHAR(30), NOT NULL): DRAFT, PENDING_PAYMENT, PROCESSING, PARTIALLY_COMPLETED, COMPLETED, CANCELLED, REFUNDED 
  - `payment_method` (VARCHAR(20), NOT NULL): COD, VNPAY, WALLET 
  - `shipping_address` (TEXT, NOT NULL): Địa chỉ nhận hàng 
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 

- **Bảng `sub_orders` (Đơn con theo từng Shop):** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Sub-Order 
  - `master_order_id` (BIGINT, NOT NULL, FK `master_orders.id`): Thuộc Master Order nào 
  - `shop_id` (BIGINT, NOT NULL): Logical FK tới Shop sở hữu 
  - `subtotal_amount` (DECIMAL(12,0), NOT NULL): Tổng tiền hàng của Shop 
  - `shipping_fee` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Phí vận chuyển của Shop 
  - `shop_voucher_discount` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Giảm giá từ Voucher riêng của Shop 
  - `final_amount` (DECIMAL(12,0), NOT NULL): Tiền cần thanh toán của Sub-Order 
  - `status` (VARCHAR(30), NOT NULL): CREATED, CONFIRMED, PACKING, SHIPPING, DELIVERED, COMPLETED, DELIVERY_FAILED, REFUNDING, REFUNDED, CANCELLED 
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 

- **Bảng `order_items`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID mục hàng 
  - `sub_order_id` (BIGINT, NOT NULL, FK `sub_orders.id`): Thuộc Sub-Order nào 
  - `variant_id` (BIGINT, NOT NULL): Logical FK tới biến thể SKU 
  - `product_name` (VARCHAR(255), NOT NULL): Snapshot tên sản phẩm tại thời điểm mua
  - `variant_name` (VARCHAR(100), NULL): Snapshot biến thể (VD: Đen - Size L)
  - `image_url` (VARCHAR(500), NULL): Snapshot ảnh đại diện sản phẩm
  - `price` (DECIMAL(12,0), NOT NULL): Giá bán tại thời điểm mua 
  - `quantity` (INT, NOT NULL): Số lượng đặt mua 

- **Bảng `refund_requests`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Khiếu nại Đổi trả
  - `sub_order_id` (BIGINT, NOT NULL, UNIQUE): Thuộc Sub-Order nào
  - `user_id` (BIGINT, NOT NULL): Người yêu cầu khiếu nại
  - `reason` (VARCHAR(255), NOT NULL): Lý do đổi trả
  - `evidence_images` (TEXT, NULL): Danh sách URL hình ảnh/video bằng chứng
  - `refund_amount` (DECIMAL(12,0), NOT NULL): Số tiền yêu cầu hoàn
  - `status` (VARCHAR(30), NOT NULL, DEFAULT 'PENDING'): PENDING, ACCEPTED, REJECTED, ADMIN_MEDIATING
  - `admin_note` (TEXT, NULL): Ý kiến phán quyết của Admin sàn
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

---

## **5.4 Payment DB (`payment_db`)** 

- **Bảng `wallets`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Ví 
  - `shop_id` (BIGINT, NOT NULL, UNIQUE): Logical FK tới `shops.id` 
  - `balance` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Số dư khả dụng có thể rút
  - `frozen_balance` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Số dư đóng băng chờ giải ngân hoặc chờ duyệt rút 

- **Bảng `escrow_accounts`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Tài khoản Escrow 
  - `sub_order_id` (BIGINT, NOT NULL, UNIQUE): Gắn với Sub-Order cụ thể 
  - `amount` (DECIMAL(12,0), NOT NULL): Số tiền tạm giữ của đơn hàng 
  - `commission_fee` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Phí hoa hồng sàn thu
  - `status` (VARCHAR(20), NOT NULL): HOLDING, RELEASED, REFUNDED 

- **Bảng `ledger_entries` (Sổ cái kép Double-Entry):** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Bút toán 
  - `transaction_id` (VARCHAR(100), NOT NULL): Mã giao dịch đối soát (Index)
  - `transaction_type` (VARCHAR(50), NOT NULL): ESCROW_HOLD, ESCROW_RELEASE, COMMISSION_FEE, WITHDRAWAL, REFUND
  - `account_debit` (VARCHAR(50), NOT NULL): Tài khoản Ghi Nợ (VD: ESCROW_HOLDING, SELLER_WALLET) 
  - `account_credit` (VARCHAR(50), NOT NULL): Tài khoản Ghi Có (VD: SYSTEM_ESCROW, PLATFORM_REVENUE) 
  - `amount` (DECIMAL(12,0), NOT NULL): Số tiền giao dịch 
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 

- **Bảng `withdrawals`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Lệnh rút tiền
  - `shop_id` (BIGINT, NOT NULL): Thuộc Shop nào
  - `amount` (DECIMAL(12,0), NOT NULL): Số tiền rút
  - `bank_code` (VARCHAR(20), NOT NULL): Ngân hàng
  - `bank_account_number` (VARCHAR(50), NOT NULL): Số tài khoản nhận
  - `bank_account_name` (VARCHAR(100), NOT NULL): Tên chủ tài khoản
  - `status` (VARCHAR(30), NOT NULL, DEFAULT 'PENDING'): PENDING, APPROVED, REJECTED, COMPLETED
  - `admin_id` (BIGINT, NULL): Admin phê duyệt
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
  - `processed_at` (TIMESTAMP, NULL): Thời gian tất toán chuyển khoản

---

## **5.5 Shipping DB (`shipping_db`)** 

- **Bảng `shipments`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Vận đơn 
  - `sub_order_id` (BIGINT, NOT NULL, UNIQUE): Gắn với Sub-Order 
  - `tracking_code` (VARCHAR(50), NOT NULL, UNIQUE): Mã vận đơn từ GHN/GHTK 
  - `carrier_name` (VARCHAR(50), NOT NULL): GHN, GHTK, MOCK_CARRIER 
  - `shipping_fee` (DECIMAL(12,0), NOT NULL, DEFAULT 0): Cước phí vận chuyển thực tế
  - `shipping_status` (VARCHAR(50), NOT NULL): READY_TO_PICK, PICKED, DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNED 
  - `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) 

- **Bảng `shipping_webhooks_log`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Log Webhook 
  - `event_id` (VARCHAR(100), UNIQUE, NOT NULL): Khóa chống trùng lặp Idempotency 
  - `carrier_name` (VARCHAR(50), NOT NULL): Tên hãng gửi webhook
  - `payload` (TEXT, NOT NULL): Dữ liệu JSON webhook nhận được 
  - `processed_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 

---

## **5.6 Notification DB (`notification_db`)** 

- **Bảng `notifications`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID thông báo 
  - `user_id` (BIGINT, NOT NULL): Logical FK tới `users.id` 
  - `title` (VARCHAR(200), NOT NULL): Tiêu đề thông báo 
  - `content` (TEXT, NOT NULL): Nội dung thông báo 
  - `is_read` (BOOLEAN, DEFAULT FALSE) 
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 

- **Bảng `email_logs`:** 
  - `id` (BIGINT, PK, AUTO_INCREMENT): ID Log Email 
  - `recipient_email` (VARCHAR(100), NOT NULL): Email người nhận 
  - `subject` (VARCHAR(255), NOT NULL): Tiêu đề thư 
  - `status` (VARCHAR(20), NOT NULL): SUCCESS, FAILED 
  - `sent_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP) 

---

# **6. Kế hoạch Phát triển & Phân công Công việc (Phương pháp MoSCoW)** 

## **6.1 Phân cấp Tính năng theo MoSCoW** 

- **Must Have (Core MVP Bắt buộc hoàn thành):** 
  - 1 API Gateway + 6 Backend Services kết nối 6 DB MySQL độc lập.
  - Phân quyền RBAC 3 vai trò: `ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`.
  - Quản lý Gian hàng (Shop Onboarding & Approval).
  - Catalog CRUD Sản phẩm, Biến thể SKU, upload ảnh MinIO S3, Redis Cache.
  - Giỏ hàng Redis gom nhóm theo Shop, tách Master Order $\rightarrow$ Sub-Orders theo Shop.
  - Saga Orchestrator kiểm tra giữ tồn kho có thời hạn (15 phút) qua RabbitMQ kèm cơ chế DLQ (`order.dlq`).
  - Thanh toán VNPay Sandbox (Tạo URL & Xử lý IPN Webhook).
  - Quản lý Escrow tạm giữ & Ví Seller theo mô hình Sổ cái kép Double-Entry.
  - Shipping Webhook xử lý Idempotency bằng `event_id`.
  - CI/CD GitHub Actions Rolling Update tự động lên VPS.
- **Should Have (Ưu tiên cao):** 
  - Áp dụng Voucher Kép (Sàn & Shop).
  - Đổi trả / Hoàn tiền một phần (`refund_requests`) trong 3 ngày.
  - Tác vụ định kỳ ShedLock: Tự động hủy đơn quá hạn 15 phút, tự động giải phóng Escrow sau 3 ngày.
  - Spring AI Chatbot tư vấn sản phẩm + Tool sinh SEO description.
  - Giám sát Metrics với Prometheus/Grafana, Tracing với Zipkin.
- **Could Have / Won't Have (Giai đoạn mở rộng tiếp theo):** 
  - Hệ thống gợi ý AI Recommendation Engine nâng cao.
  - Livestream Bán hàng & Flash Sale Engine chịu tải 10.000 RPS.

---

## **6.2 Phân công Trách nhiệm Thành viên** 

| Thành viên | Trách nhiệm chính | Chi tiết công việc triển khai |
|---|---|---|
| **Lê Đức Anh** | DevOps & Infrastructure, Identity & Shop, API Gateway, AI Assistant | • Khởi tạo Repo, cấu hình Docker Compose toàn bộ hạ tầng (6 MySQL DBs, Redis, RabbitMQ, MinIO, Zipkin, Prometheus, Grafana).<br>• Phát triển API Gateway: Dual Auth (HttpOnly Cookie & Bearer Header), RBAC Authorization Filter, Rate Limiting ưu tiên `CF-Connecting-IP`.<br>• Phát triển Identity Service: Auth REST API, Quản lý hồ sơ Shop (`shops`), KYC và phê duyệt Seller.<br>• Viết GitHub Actions CI/CD Script deploy tự động lên VPS.<br>• Tích hợp Spring AI Chatbot tư vấn sản phẩm và hỗ trợ viết bài SEO. |
| **Triệu Quốc Bình** | Frontend Layout, Catalog & Product Service, Storage MinIO | • Dựng Giao diện Frontend ReactJS + Tailwind CSS Responsive (Storefront, Seller Center, Admin Dashboard).<br>• Phát triển Catalog Service: CRUD Danh mục, Sản phẩm, Biến thể SKU, đánh giá sao sản phẩm (`product_reviews`).<br>• Tích hợp Redis Caching cho Catalog Service tối ưu tốc độ đọc $< 100ms$.<br>• Cấu hình MinIO Object Storage Server & SDK Upload Image.<br>• Phát triển Notification Service: Lắng nghe Async Message từ RabbitMQ gửi Mail OTP và thông báo. |
| **Nguyễn Văn Kiên** | Order & Cart, Payment & Wallet, Shipping & Logistics, E2E Testing | • Xây dựng Giỏ hàng Redis Cart Server gom nhóm theo Shop + Logic Merge Giỏ hàng Guest-User.<br>• Phát triển Order Service: Luồng Checkout tách Master/Sub-Orders, Saga Orchestrator qua RabbitMQ kèm Dead Letter Queue (`order.dlq`), quản lý Voucher và Khiếu nại Hoàn tiền.<br>• Phát triển Payment Service: Cổng thanh toán VNPay, Escrow Account, Double-Entry Ledger, Ví Seller và Lệnh rút tiền.<br>• Phát triển Shipping Service: Idempotent Webhook xử lý mã vận đơn GHN/GHTK.<br>• Viết Kịch bản E2E Integration Testing kiểm thử toàn bộ luồng hệ thống. |

---

## **6.3 Lịch trình Triển khai Chi tiết (Timeline 7 ngày)** 

- **Ngày 01 - 02: Khởi động Hạ tầng, Đồng bộ 6 DBs & Core Architecture** 
  - _Lê Đức Anh:_ Setup Docker Compose (6 MySQL DBs, Redis, RabbitMQ, MinIO, Zipkin), hoàn thiện API Gateway Skeleton (Dual Auth & Route đầy đủ 6 services), hoàn thiện Identity Service (User, Shop, RBAC). 
  - _Triệu Quốc Bình:_ Dựng bộ khung Frontend ReactJS Storefront & Dashboard, phát triển Catalog Service (Category, Product, Variant, MinIO upload). 
  - _Nguyễn Văn Kiên:_ Khởi tạo schema cơ sở dữ liệu cho 6 Services, xây dựng Redis Cart Server gom nhóm theo Shop, cấu hình RabbitMQ Exchanges/Queues/DLQ.
- **Ngày 03: Tích hợp Gateway, Caching & Asynchronous Messaging** 
  - _Lê Đức Anh:_ Cấu hình Auth Filter, kiểm tra RBAC và Rate Limiting tại Gateway; thiết lập VPS Ubuntu, Nginx Reverse Proxy và SSL. 
  - _Triệu Quốc Bình:_ Tích hợp Redis Caching cho Catalog Service, hoàn thiện giao diện danh sách sản phẩm, chi tiết sản phẩm và bộ lọc tìm kiếm. 
  - _Nguyễn Văn Kiên:_ Thiết lập kịch bản Saga Orchestrator cho Order Service kết nối Catalog Service (giữ kho 15 phút), hoàn thiện logic đồng bộ giỏ hàng Guest-to-User.
- **Ngày 04 - 05: Giao dịch phân tán Saga, Thanh toán & Trợ lý AI** 
  - _Lê Đức Anh:_ Viết GitHub Actions CI/CD Pipeline (Build Docker Image, push Hub, auto-deploy lên VPS với Rolling Update); tích hợp Spring AI Chatbot và công cụ sinh mô tả sản phẩm chuẩn SEO. 
  - _Triệu Quốc Bình:_ Hoàn thiện Notification Service (gửi email OTP qua RabbitMQ), dựng giao diện quản trị Admin Portal và tích hợp AI Chatbot Widget vào giao diện Storefront. 
  - _Nguyễn Văn Kiên:_ Hoàn thiện luồng Saga tách Master Order/Sub-Orders, tích hợp cổng thanh toán VNPay Sandbox, xây dựng cơ chế Escrow Double-Entry Ledger, Shipping Webhook Idempotency và RabbitMQ Dead Letter Queue.
- **Ngày 06 - 07: Observability, E2E Testing, Tối ưu & Bàn giao** 
  - _Lê Đức Anh:_ Thiết lập hệ thống giám sát tập trung (Prometheus, Grafana Dashboards, Loki Logging, Zipkin Tracing) và cấu hình cảnh báo sự cố. 
  - _Cả nhóm (Đức Anh, Quốc Bình, Văn Kiên):_ Chạy kịch bản kiểm thử tích hợp đầu cuối (E2E Integration Testing), mô phỏng tải đồng thời (Load Test 1.000 RPS), khắc phục các lỗi phát sinh, rà soát tính toàn vẹn dữ liệu và đóng gói bàn giao sản phẩm.
