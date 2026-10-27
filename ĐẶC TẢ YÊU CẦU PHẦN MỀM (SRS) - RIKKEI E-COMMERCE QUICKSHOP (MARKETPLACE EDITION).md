# **ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS) \- RIKKEI E-COMMERCE QUICKSHOP (MARKETPLACE EDITION)**

 

**Sàn Thương mại Điện tử Đa người bán (Multi-Vendor Marketplace) \- Microservices & DevOps Production-Ready**

 

# **0\. Lịch sử thay đổi tài liệu (Document Revision History)**

 

| Phiên bản | Ngày | Tác giả | Tóm tắt thay đổi |
| :---- | :---- | :---- | :---- |
| 1.0.0 | 15/10/2026 | Lê Đức Anh | Khởi tạo tài liệu đặc tả ban đầu. |
| 2.0.0 | 21/10/2026 | Lê Đức Anh | Nâng cấp kiến trúc Microservices & chuẩn hóa DevOps. |
| 2.1.0 | 21/10/2026 | Lê Đức Anh | Bổ sung State Machine và thiết kế luồng Saga Orchestrator. |
| **2.2.0** | **21/10/2026** | **Lê Đức Anh** | **Chuẩn hóa toàn diện:** Thống nhất mô hình 7 services (1 Gateway \+ 6 Backend), chuẩn hóa cổng dịch vụ (Shipping 8085, Notification 8086), bổ sung các bảng CSDL còn thiếu (vouchers, product\_reviews, refund\_requests, withdrawals), hoàn thiện State Machine (thêm trạng thái giao thất bại/hoàn hàng, hủy đơn), tích hợp cơ chế Dual Auth (HttpOnly Cookie & Bearer Header) và phân định ranh giới Core Production vs Mock Simulators. |

   
 

 

# **1\. Giới thiệu**

 

## **1.1 Mục đích**

Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này xác định toàn bộ các yêu cầu chức năng, phi chức năng, kiến trúc kỹ thuật và phương án triển khai cho sàn thương mại điện tử đa người bán (Multi-vendor Marketplace) **Rikkei E-Commerce QuickShop** theo mô hình hoạt động tương tự Shopee. Hệ thống được thiết kế theo chuẩn mã nguồn mở cấp độ Production, đảm bảo khả năng chịu tải cao, mở rộng linh hoạt theo kiến trúc Microservices (Database-per-Service) và tự động hóa toàn diện quy trình vận hành qua DevOps/CI-CD.

 

## **1.2 Phạm vi sản phẩm & Chuẩn hóa Thuật ngữ**

Hệ thống cung cấp nền tảng kết nối toàn diện cho 4 nhóm đối tượng chính:

 

* **Khách hàng (Buyer):** Tìm kiếm/lọc sản phẩm đa gian hàng, quản lý giỏ hàng tách theo Shop, áp dụng Voucher kép (Sàn & Shop), thanh toán trực tuyến qua cổng VNPay/Ví điện tử/COD, theo dõi vận chuyển đơn hàng thời gian thực, tương tác AI Chatbot tư vấn, đánh giá/nhận xét gian hàng và yêu cầu Đổi trả/Hoàn tiền (Refund/Return).

* **Người bán (Seller):** Đăng ký và quản lý hồ sơ Shop, quản lý danh mục & sản phẩm đa biến thể SKU, tiếp nhận và xử lý luồng đơn hàng con (Sub-Orders), kết nối đơn vị vận chuyển (GHN/GHTK), quản lý Ví Seller/Sổ cái dòng tiền kép (Double-Entry Ledger), đối soát tài chính và rút tiền về tài khoản ngân hàng, tạo chiến dịch khuyến mãi (Voucher Shop/Flash Sale) và sinh nội dung SEO tự động bằng AI.

* **Quản trị viên (Admin):** Duyệt/Khóa hồ sơ mở Shop, kiểm duyệt sản phẩm mới, quản lý cây danh mục chung, điều phối và phân xử tranh chấp/khiếu nại hoàn tiền, cấu hình mức phí hoa hồng (Commission Fee), quản lý Voucher toàn sàn, cấu hình Banner/Trang chủ, tra cứu Audit Log và theo dõi báo cáo doanh thu/dòng tiền Marketplace.

* **Vận hành (Ops/System):** Tích hợp hạ tầng lưu trữ Object Storage (MinIO S3), xác thực an toàn chuẩn JWT (hỗ trợ cả HttpOnly Secure Cookie lẫn Bearer Authorization Header) / RBAC, quản lý webhook idempotency kết nối đối tác vận chuyển, chạy các tác vụ định kỳ ShedLock, tự động hóa CI/CD triển khai Zero-Downtime và giám sát Log/Metrics/Tracing tập trung (Prometheus, Grafana, Loki, Zipkin).

 

## **1.2.1 Chuẩn hóa Trạng thái & State Machine Toàn hệ thống**

 

* **Trạng thái Người dùng (User Status):**  
  PENDING\_VERIFICATION (Chờ kích hoạt OTP) ![][image1] ACTIVE (Đang hoạt động) ![][image1] BLOCKED (Khóa tài khoản do vi phạm).

* **Trạng thái Gian hàng (Shop Status):**  
  PENDING\_APPROVAL (Chờ Admin duyệt) ![][image1] ACTIVE (Đang hoạt động kinh doanh) ![][image1] SUSPENDED (Tạm dừng/Khóa shop) / REJECTED (Từ chối duyệt hồ sơ).

* **State Machine Master Order (Đơn hàng Tổng):**  
  DRAFT ![][image1] PENDING\_PAYMENT ![][image1] PROCESSING ![][image1] PARTIALLY\_COMPLETED / COMPLETED / CANCELLED / REFUNDED.

* **State Machine Sub-Order (Đơn con theo từng Shop):**  
  CREATED ![][image1] CONFIRMED (Shop xác nhận) ![][image1] PACKING (Đang đóng gói) ![][image1] SHIPPING (Đang giao hàng) ![][image1] DELIVERED (Giao thành công) ![][image1] COMPLETED (Đã giải phóng tiền Escrow vào ví Seller).

  * *Nhánh hoàn / hủy / khiếu nại:*

    * CANCELLED: Khách hủy khi chưa giao hoặc quá hạn thanh toán 15 phút.

    * DELIVERY\_FAILED: Giao thất bại / Bom hàng ![][image1] chuyển hoàn kho Shop.

    * REFUNDING ![][image1] REFUNDED: Khách khiếu nại đổi trả được chấp thuận, kích hoạt hoàn tiền và đóng băng Escrow.

 

 

## **1.3 Công nghệ sử dụng**

 

| Tầng kiến trúc | Công nghệ áp dụng | Vai trò & Chi tiết triển khai |
| :---- | :---- | :---- |
| **Frontend** | ReactJS 18, Zustand, Tailwind CSS, Lucide Icons | SPA Storefront \+ Seller Center \+ Admin Dashboard, quản lý Client State tối ưu, UI Responsive |
| **Backend API** | Spring Boot 3.3.x, Java 17 | Xây dựng các Microservices độc lập, ShedLock cho Scheduled Tasks |
| **Microservices Framework** | Spring Cloud Gateway, OpenFeign, Resilience4j | API Routing, Rate Limiting (Token Bucket), Circuit Breaker, Inter-service Communication |
| **Security & AI** | Spring Security 6, JWT (HttpOnly Cookie & Bearer Header), Spring AI | Phân quyền RBAC (ROLE\_BUYER, ROLE\_SELLER, ROLE\_ADMIN), chống Prompt Injection, Fallback AI |
| **Database** | MySQL 8.0 (Database per Service) | 6 Database độc lập cho 6 backend business services |
| **Caching & Messaging** | Redis 7, RabbitMQ 3.13 | Caching giỏ hàng/sản phẩm, Distributed Lock, Async Messaging (Saga Orchestrator Pattern, DLQ order.dlq) |
| **Storage & Static** | MinIO (S3 Compatible) | Object Storage cho hình ảnh sản phẩm, media gian hàng |
| **DevOps & Infra** | Docker, Nginx, GitHub Actions, Cloudflare | Containerization, Reverse Proxy, CI/CD Rolling Deployment, SSL & DDoS Protection |
| **Monitoring & Tracing** | Prometheus, Grafana, Loki, Micrometer/Zipkin | Giám sát chỉ số tài nguyên, log tập trung và Distributed Tracing (Correlation ID) |

   
 

 

# **2\. Kiến trúc Hệ thống (Microservices Architecture)**

 

Hệ thống Marketplace được thiết kế theo mô hình Microservices phân tán với **1 API Gateway và 6 Dịch vụ Backend cốt lõi**, tách biệt hoàn toàn cơ sở dữ liệu (Database-per-Service):

 

1\. **API Gateway (Port 8080):**  
Cổng giao tiếp duy nhất cho Storefront, Seller Center và Admin Portal. Đảm nhận API Routing, kiểm tra JWT Token (hỗ trợ trích xuất cả từ HttpOnly Cookie lẫn Bearer Header), phân quyền RBAC (BUYER, SELLER, ADMIN), áp dụng Rate Limiting chống DDoS dựa trên header CF-Connecting-IP (fallback X-Forwarded-For).

2\. **Identity & Shop Service (Port 8081):**  
Quản lý tài khoản đa vai trò, quy trình KYC/Đăng ký Gian hàng, phê duyệt hồ sơ Seller, phân quyền RBAC và quản lý thông tin Profile Shop.  
*Cơ sở dữ liệu:* identity\_db.

3\. **Catalog & Product Service (Port 8082):**  
Quản lý cây danh mục chung, đăng bán sản phẩm thuộc gian hàng, kiểm duyệt bài đăng, biến thể SKU, quản lý tồn kho và upload media lên MinIO S3 Server. Đánh giá nhận xét sản phẩm (product\_reviews).  
*Cơ sở dữ liệu:* catalog\_db.

4\. **Order & Cart Service (Port 8083):**  
Quản lý giỏ hàng đa Shop trên Redis, tính toán giá trị đơn, áp dụng Voucher kép (Sàn \+ Shop), tách đơn hàng tự động thành 1 Master Order và các Sub-Orders theo từng Shop. Đóng vai trò **Saga Orchestrator** điều phối toàn bộ chuỗi giao dịch phân tán qua RabbitMQ kèm cơ chế Dead Letter Queue (order.dlq). Quản lý yêu cầu đổi trả/hoàn tiền (refund\_requests).  
*Cơ sở dữ liệu:* order\_db.

5\. **Payment & Wallet Service (Port 8084):**  
Tích hợp cổng VNPay Sandbox (IPN & Return), quản lý Tài khoản Tạm giữ (Escrow) của Sàn cho cả thanh toán VNPay và COD, duy trì Sổ cái kép (Double-Entry Ledger) cho Ví Seller, tự động khấu trừ hoa hồng sàn (Commission Fee), quản lý các lệnh rút tiền (withdrawals).  
*Cơ sở dữ liệu:* payment\_db.

6\. **Shipping & Logistics Service (Port 8085):**  
Tích hợp API đơn vị vận chuyển (GHN/GHTK hoặc Mock Logistics Simulator), tính phí ship chuẩn xác theo địa chỉ, sinh mã vận đơn và tiếp nhận Idempotent Webhook cập nhật hành trình đơn hàng dựa trên event\_id.  
*Cơ sở dữ liệu:* shipping\_db.

7\. **Notification & AI Service (Port 8086):**  
Lắng nghe sự kiện qua RabbitMQ gửi Email OTP/Thông báo đơn hàng. Vận hành AI Chatbot tư vấn sản phẩm (chống Prompt Injection) và công cụ AI hỗ trợ Seller sinh bài mô tả chuẩn SEO.  
*Cơ sở dữ liệu:* notification\_db.

 

 

# **3\. Yêu cầu Chức năng (Functional Requirements)**

 

## **3.1 Phân hệ Xác thực & Người dùng (Identity Module)**

 

* **FR-01: Đăng ký & Đăng nhập (Authentication):**

  * **Đăng ký:** Họ tên, Email, Mật khẩu (tối thiểu 8 ký tự, có chữ hoa, chữ số, ký tự đặc biệt). Kiểm tra trùng lặp Email. Mật khẩu mã hóa BCrypt. Tài khoản tạo ở trạng thái PENDING\_VERIFICATION, OTP 6 số lưu Redis (TTL \= 5 phút), gửi mail kích hoạt qua RabbitMQ.

  * **Đăng nhập:** Kiểm tra mật khẩu và trạng thái ACTIVE. Cấp Access Token (short-lived) và Refresh Token. Hệ thống hỗ trợ thiết lập Refresh Token vào HttpOnly Secure SameSite Cookie và trả Access Token qua AuthResponse.

* **FR-02: Làm mới Token & Quên mật khẩu:**

  * **Refresh Token:** Gateway verify token, kiểm tra tính hợp lệ trong Redis Blacklist. Cấp Access Token mới nếu hợp lệ.

  * **Quên mật khẩu:** Nhận Email ![][image1] Tạo token reset lưu trong Redis (TTL 15 phút) ![][image1] Gửi link/OTP qua Mail ![][image1] Người dùng nhập mật khẩu mới ![][image1] Cập nhật Database.

* **FR-02.1: Đăng ký & Quản lý Gian hàng (Seller Onboarding & Shop Profile):**

  * Cho phép người dùng chuyển đổi tài khoản thành Người bán, cập nhật hồ sơ định danh KYC (CCCD/MST, Tên Shop, Logo, Kho lấy hàng, Tài khoản ngân hàng rút tiền).

  * Bản ghi trạng thái PENDING\_APPROVAL. Admin duyệt chuyển thành ACTIVE, gán thêm quyền ROLE\_SELLER, tự động kích hoạt tạo Ví Seller bên Payment Service và mở quyền truy cập Seller Center.

 

 

## **3.2 Phân hệ Sản phẩm & Danh mục (Catalog Module)**

 

* **FR-03: Tìm kiếm, Lọc & Caching Sản phẩm:**

  * Khách hàng tìm kiếm sản phẩm đa gian hàng theo từ khóa, danh mục sàn, khoảng giá, đánh giá sao, và tìm kiếm riêng theo từng Shop.

  * Kiểm tra Redis Cache. Nếu Cache Miss: truy vấn Catalog DB có Index, ghi lại vào Redis với TTL \= 10 phút. Phân trang chuẩn RESTful. Tốc độ ![][image2] khi Cache Hit.

* **FR-04: Quản lý Biến thể, Upload Ảnh MinIO & Kiểm duyệt Sản phẩm:**

  * Người bán đăng tải và quản lý sản phẩm thuộc Shop mình với nhiều biến thể SKU (Màu sắc, Size, Giá, Tồn kho thực tế và Tồn kho đang giữ thanh toán).

  * Upload file media lên MinIO S3 Server qua SDK, nhận public URL. Lưu thông tin vào products và product\_variants.

  * Invalidate Cache liên quan trong Redis. Sản phẩm tạo ở trạng thái PENDING\_APPROVAL, sau khi Admin duyệt chuyển sang ACTIVE và xuất hiện trên Storefront.

* **FR-04.1: Đánh giá & Nhận xét Sản phẩm (Reviews & Ratings):**

  * Khách hàng sau khi đơn hàng Sub-Order chuyển trạng thái COMPLETED có quyền đánh giá 1-5 sao và để lại nhận xét kèm hình ảnh cho sản phẩm.

 

 

## **3.3 Phân hệ Giỏ hàng (Cart Module)**

 

* **FR-05: Quản lý & Đồng bộ Giỏ hàng (Guest to User):**

  * Gom nhóm mặt hàng theo từng Shop riêng biệt (shop\_id), hỗ trợ tick chọn từng Shop để tiến hành checkout độc lập.

  * **Guest Cart:** Lưu ở LocalStorage trình duyệt.

  * **User Cart:** Lưu trong Redis Hash Key cart:{user\_id}, field là variant\_id, value là JSON chứa thông tin số lượng và shop.

  * **Quy tắc giải quyết xung đột khi hợp nhất giỏ hàng (Cart Merge Conflict Resolution):** Khi Guest đăng nhập, hệ thống duyệt từng mặt hàng trong Guest Cart:

    * Nếu mặt hàng (SKU) chưa có trong giỏ hàng User: Thêm mới toàn bộ số lượng vào Redis Cart.

    * Nếu mặt hàng (SKU) đã tồn tại trong giỏ hàng User: Cộng dồn số lượng ($qt{y}_{user}=qt{y}_{user}+qt{y}_{guest}$). Nếu vượt quá tồn kho khả dụng hiện tại, điều chỉnh về mức tồn kho tối đa khả dụng và kèm thông báo cho khách hàng.

 

 

## **3.4 Phân hệ Đơn hàng & Thanh toán (Order & Payment Module)**

 

* **FR-06: Đặt hàng, Voucher Kép & Saga Orchestrator Workflow:**

  * Buyer Checkout giỏ hàng đa Shop. Hệ thống tách tự động thành 1 Master Order và các Sub-Orders theo từng Shop.

  * Order Service đóng vai trò **Saga Orchestrator**:

  1\. Tạo Master Order và Sub-Orders (DRAFT). Lưu Snapshot giá và thông tin sản phẩm (product\_name, variant\_name, image\_url).

  2\. Gửi lệnh giữ kho (Reserve Stock có TTL 15 phút) sang Catalog Service.

  3\. Kiểm tra tính hợp lệ của Voucher Sàn và Voucher từng Shop từ bảng vouchers.

  4\. Nếu giữ kho và voucher thành công: Tạo giao dịch thanh toán (VNPay URL hoặc xác nhận COD). Chuyển trạng thái sang PENDING\_PAYMENT.

  5\. Nếu thất bại hoặc hết hạn 15 phút không thanh toán: Kích hoạt Compensating Transaction để hoàn lại kho đã giữ và hủy đơn hàng (CANCELLED).

  6\. **Cơ chế Dead Letter Queue (DLQ):** Toàn bộ các thông điệp giữ kho / bù trừ kho thất bại sau 3 lần retry sẽ tự động đẩy vào RabbitMQ Dead Letter Queue (order.dlq) để kích hoạt cảnh báo cho kỹ sư vận hành xử lý thủ công.

* **FR-07: Cổng Thanh toán VNPay & Idempotent Shipping Webhook:**

  * Xử lý VNPay IPN Webhook với chữ ký bảo mật HMAC-SHA512.

  * Đơn COD: Số tiền thu hộ được đối soát định kỳ và ghi nhận vào tài khoản COD Escrow.

  * Tiếp nhận Webhook từ GHN/GHTK: Áp dụng cơ chế Idempotency qua event\_id để tránh xử lý lặp trạng thái vận đơn.

* **FR-07.1: Quản lý Escrow, Double-Entry Ledger, Phí Hoa hồng & Rút tiền Seller:**

  * Áp dụng mô hình Sổ cái kép (Double-Entry Ledger) chặt chẽ: Nợ tài khoản Escrow sàn, Có tài khoản Ví Seller.

  * Tự động khấu trừ Phí hoa hồng sàn (ví dụ: 3% \- cấu hình linh hoạt bởi Admin) và phí vận chuyển.

  * **Điều kiện giải phóng Escrow:** Sau 3 ngày kể từ khi đơn hàng ở trạng thái DELIVERED mà không có khiếu nại, hoặc Buyer ấn xác nhận "Đã nhận được hàng".

  * Seller tạo lệnh Rút tiền (withdrawals) ![][image1] Admin duyệt ![][image1] Chuyển khoản và hạch toán trừ số dư ví khả dụng.

* **FR-07.2: Quản lý Đổi trả / Khiếu nại (Refund & Return Workflow):**

  * Khách hàng có quyền yêu cầu Đổi trả/Hoàn tiền cho từng Sub-Order trong vòng 3 ngày kể từ khi giao hàng thành công (lưu vào bảng refund\_requests).

  * Đóng băng quá trình giải phóng tiền Escrow của Sub-Order đó. Hỗ trợ Hoàn tiền một phần (Partial Refund) độc lập, không làm gián đoạn các Sub-Orders khác trong cùng Master Order.

* **FR-07.3: Các Tác vụ Định kỳ Hệ thống (Scheduled Tasks với ShedLock):**

  * Tự động Hủy đơn chưa thanh toán: Quét mỗi 5 phút các đơn PENDING\_PAYMENT quá 15 phút để nhả kho giữ.

  * Tự động Giải phóng Escrow: Chạy lúc 00:00 hàng ngày, chuyển tiền vào ví Seller cho các Sub-Order giao thành công quá 3 ngày không khiếu nại.

  * Toàn bộ Scheduled Tasks được đồng bộ bằng Redis ShedLock chống chạy trùng giữa các container instance.

 

 

## **3.5 Phân hệ Quản trị (Admin Module)**

* **FR-08: Báo cáo Thống kê Doanh thu:** Tổng quan kinh doanh Marketplace, tổng GMV toàn sàn, tổng phí hoa hồng thu được và doanh thu theo từng Shop.

* **FR-09: Quản lý Hệ thống, Audit Log, Banner & Phân xử Tranh chấp:** Quản lý tài khoản, duyệt gian hàng, kiểm duyệt bài đăng sản phẩm, cấu hình danh mục sàn, xử lý khiếu nại hoàn tiền và lưu vết Audit Log.

 

 

## **3.6 Phân hệ Trợ lý AI (AI Module \- Spring AI)**

* **FR-10: AI Chatbot Tư vấn Mua sắm (An toàn & Chống Prompt Injection):**

  * Chatbot tư vấn sản phẩm đa gian hàng theo thời gian thực.

  * Làm sạch câu hỏi đầu vào (Prompt Sanitization) chống tấn công Prompt Injection.

  * Gọi Spring AI Function Calling truy vấn Catalog Service. Cơ chế Circuit Breaker & Fallback trả kết quả tìm kiếm mặc định khi LLM timeout.

* **FR-11: Sinh Mô tả Sản phẩm Chuẩn SEO:** Hỗ trợ Seller sinh bài mô tả chuẩn SEO dạng Markdown/HTML từ các thông số kỹ thuật ngắn gọn.

 

 

# **4\. Yêu cầu Phi chức năng (Non-Functional Requirements)**

 

## **4.1 Hiệu năng (Performance)**

* **Response Time:** API đọc ![][image3] (Cache Hit), ![][image4] (Cache Miss). API ghi dữ liệu ![][image5].

* **Throughput:** Đạt tối thiểu 1.000 RPS đỉnh điểm.

* **Tối ưu Caching:** 90% lượt truy vấn danh sách sản phẩm được phục vụ trực tiếp từ Redis.

 

## **4.2 Bảo mật (Security)**

* Toàn bộ kết nối sử dụng HTTPS (TLS 1.3). JWT Tokens lưu trong HttpOnly Secure SameSite Cookies (hoặc Bearer Authorization Header) chống tấn công XSS.

* Rate Limiting thuật toán Token Bucket tại Gateway dựa theo header CF-Connecting-IP.

* Chống OWASP Top 10: SQL Injection (JPA PreparedStatement), XSS, CSRF.

* Hạ tầng mạng cô lập: Các Database (MySQL), Redis, RabbitMQ chỉ lắng nghe trong Docker Internal Network, tuyệt đối không public port ra ngoài Internet.

 

## **4.3 Quy trình CI/CD & DevOps**

* Developer push code lên nhánh main trên GitHub.

* GitHub Actions tự động kích hoạt: Run Unit Test ![][image1] Build JAR File ![][image1] Build Docker Image ![][image1] Push Image lên Docker Hub.

* Triển khai Zero-Downtime Deployment bằng kỹ thuật Rolling Update / Blue-Green thông qua Docker Compose & Nginx Upstream Reload.

 

## **4.4 Giám sát & Quản lý Log (Monitoring & Observability)**

* **Loki Driver:** Thu thập toàn bộ log stdout/stderr của Docker Container.

* **Prometheus & Grafana:** Thu thập số liệu tài nguyên (CPU, RAM, Request Rate, Error Rate 5xx) và gửi cảnh báo Telegram/Slack.

* **Micrometer & Zipkin:** Gắn Correlation ID (X-Correlation-Id) xuyên suốt qua các cuộc gọi HTTP và Message RabbitMQ để truy vết phân tán (Distributed Tracing).

 

 

# **5\. Thiết kế Cơ sở dữ liệu Chuẩn (Database-per-Service Architecture)**

 

Mỗi Microservice sở hữu một Database MySQL riêng biệt. Toàn bộ các giá trị tiền tệ được chuẩn hóa bằng kiểu DECIMAL(12,0) phù hợp cho Việt Nam Đồng (VND).

 

## **5.1 Identity DB (identity\_db)**

 

* **Bảng users:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID người dùng

  * username (VARCHAR(50), UNIQUE, NOT NULL): Tên đăng nhập

  * email (VARCHAR(100), UNIQUE, NOT NULL): Email đăng nhập

  * password (VARCHAR(255), NOT NULL): Mật khẩu mã hóa BCrypt

  * full\_name (VARCHAR(100), NOT NULL): Họ và tên

  * phone\_number (VARCHAR(20), NULL): Số điện thoại

  * role (VARCHAR(30), NOT NULL, DEFAULT 'ROLE\_BUYER'): ROLE\_BUYER, ROLE\_SELLER, ROLE\_ADMIN

  * status (VARCHAR(30), NOT NULL, DEFAULT 'PENDING\_VERIFICATION'): PENDING\_VERIFICATION, ACTIVE, BLOCKED

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

  * updated\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP)

* **Bảng shops:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Gian hàng

  * owner\_id (BIGINT, NOT NULL, UNIQUE): Ref tới users.id

  * shop\_name (VARCHAR(100), NOT NULL, UNIQUE): Tên gian hàng

  * tax\_code (VARCHAR(50), NULL): Mã số thuế / CCCD

  * phone\_number (VARCHAR(20), NULL): Hotline gian hàng

  * bank\_code (VARCHAR(20), NULL): Ngân hàng nhận tiền (VCB, MB, TCB...)

  * bank\_account\_number (VARCHAR(50), NULL): Số tài khoản ngân hàng

  * bank\_account\_name (VARCHAR(100), NULL): Tên chủ tài khoản

  * status (VARCHAR(30), NOT NULL, DEFAULT 'PENDING\_APPROVAL'): PENDING\_APPROVAL, ACTIVE, SUSPENDED, REJECTED

  * warehouse\_address (TEXT, NOT NULL): Địa chỉ kho lấy hàng

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

 

 

## **5.2 Catalog DB (catalog\_db)**

 

* **Bảng categories:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Danh mục

  * name (VARCHAR(100), NOT NULL): Tên danh mục

  * slug (VARCHAR(100), UNIQUE, NOT NULL): Đường dẫn SEO

* **Bảng products:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Sản phẩm

  * shop\_id (BIGINT, NOT NULL): Logical FK tới shops.id

  * category\_id (BIGINT, NOT NULL): Logical FK tới categories.id

  * name (VARCHAR(255), NOT NULL): Tên sản phẩm

  * description (MEDIUMTEXT, NULL): Mô tả chi tiết HTML/Markdown (chứa tối đa 16MB)

  * main\_image (VARCHAR(500), NULL): URL ảnh trên MinIO

  * status (VARCHAR(30), NOT NULL, DEFAULT 'PENDING\_APPROVAL'): PENDING\_APPROVAL, ACTIVE, REJECTED

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

* **Bảng product\_variants:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Biến thể

  * product\_id (BIGINT, NOT NULL, FK products.id): Thuộc sản phẩm nào

  * sku (VARCHAR(50), UNIQUE, NOT NULL): Mã SKU kho hàng

  * color (VARCHAR(50), NULL): Màu sắc

  * size (VARCHAR(20), NULL): Kích cỡ

  * price (DECIMAL(12,0), NOT NULL): Giá bán VND

  * stock (INT, NOT NULL, DEFAULT 0): Tồn kho thực tế

  * reserved\_stock (INT, NOT NULL, DEFAULT 0): Tồn kho đang giữ thanh toán

* **Bảng product\_reviews:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Đánh giá

  * product\_id (BIGINT, NOT NULL, FK products.id): Đánh giá sản phẩm nào

  * sub\_order\_id (BIGINT, NOT NULL): Gắn với Sub-Order cụ thể

  * user\_id (BIGINT, NOT NULL): Người đánh giá

  * rating (TINYINT, NOT NULL CHECK (rating BETWEEN 1 AND 5)): Số sao đánh giá (1-5)

  * comment (TEXT, NULL): Nội dung nhận xét

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

 

 

## **5.3 Order DB (order\_db)**

 

* **Bảng vouchers:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Voucher

  * code (VARCHAR(50), UNIQUE, NOT NULL): Mã voucher (VD: QUICKSHOP10, SHOPVIP20)

  * shop\_id (BIGINT, NULL): NULL nếu là Voucher toàn sàn, có ID nếu là Voucher của Shop

  * discount\_type (VARCHAR(20), NOT NULL): PERCENTAGE, FIXED\_AMOUNT

  * discount\_value (DECIMAL(12,0), NOT NULL): Giá trị giảm (% hoặc số tiền)

  * min\_order\_value (DECIMAL(12,0), NOT NULL, DEFAULT 0): Giá trị đơn hàng tối thiểu

  * max\_discount\_amount (DECIMAL(12,0), NULL): Mức giảm tối đa với Voucher %

  * usage\_limit (INT, NOT NULL): Tổng lượt dùng tối đa

  * used\_count (INT, NOT NULL, DEFAULT 0): Số lượt đã dùng

  * start\_date (TIMESTAMP, NOT NULL): Ngày bắt đầu

  * end\_date (TIMESTAMP, NOT NULL): Ngày hết hạn

  * status (VARCHAR(20), NOT NULL, DEFAULT 'ACTIVE'): ACTIVE, INACTIVE, EXPIRED

* **Bảng master\_orders (Đơn hàng Tổng):**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Master Order

  * user\_id (BIGINT, NOT NULL): Logical FK tới users.id

  * total\_amount (DECIMAL(12,0), NOT NULL): Tổng thanh toán toàn đơn (VND)

  * voucher\_discount (DECIMAL(12,0), NOT NULL, DEFAULT 0): Giảm giá từ Voucher sàn

  * shipping\_fee\_total (DECIMAL(12,0), NOT NULL, DEFAULT 0): Tổng phí vận chuyển các shop

  * status (VARCHAR(30), NOT NULL): DRAFT, PENDING\_PAYMENT, PROCESSING, PARTIALLY\_COMPLETED, COMPLETED, CANCELLED, REFUNDED

  * payment\_method (VARCHAR(20), NOT NULL): COD, VNPAY, WALLET

  * shipping\_address (TEXT, NOT NULL): Địa chỉ nhận hàng

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

* **Bảng sub\_orders (Đơn con theo từng Shop):**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Sub-Order

  * master\_order\_id (BIGINT, NOT NULL, FK master\_orders.id): Thuộc Master Order nào

  * shop\_id (BIGINT, NOT NULL): Logical FK tới Shop sở hữu

  * subtotal\_amount (DECIMAL(12,0), NOT NULL): Tổng tiền hàng của Shop

  * shipping\_fee (DECIMAL(12,0), NOT NULL, DEFAULT 0): Phí vận chuyển của Shop

  * shop\_voucher\_discount (DECIMAL(12,0), NOT NULL, DEFAULT 0): Giảm giá từ Voucher riêng của Shop

  * final\_amount (DECIMAL(12,0), NOT NULL): Tiền cần thanh toán của Sub-Order

  * status (VARCHAR(30), NOT NULL): CREATED, CONFIRMED, PACKING, SHIPPING, DELIVERED, COMPLETED, DELIVERY\_FAILED, REFUNDING, REFUNDED, CANCELLED

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

* **Bảng order\_items:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID mục hàng

  * sub\_order\_id (BIGINT, NOT NULL, FK sub\_orders.id): Thuộc Sub-Order nào

  * variant\_id (BIGINT, NOT NULL): Logical FK tới biến thể SKU

  * product\_name (VARCHAR(255), NOT NULL): Snapshot tên sản phẩm tại thời điểm mua

  * variant\_name (VARCHAR(100), NULL): Snapshot biến thể (VD: Đen \- Size L)

  * image\_url (VARCHAR(500), NULL): Snapshot ảnh đại diện sản phẩm

  * price (DECIMAL(12,0), NOT NULL): Giá bán tại thời điểm mua

  * quantity (INT, NOT NULL): Số lượng đặt mua

* **Bảng refund\_requests:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Khiếu nại Đổi trả

  * sub\_order\_id (BIGINT, NOT NULL, UNIQUE): Thuộc Sub-Order nào

  * user\_id (BIGINT, NOT NULL): Người yêu cầu khiếu nại

  * reason (VARCHAR(255), NOT NULL): Lý do đổi trả

  * evidence\_images (TEXT, NULL): Danh sách URL hình ảnh/video bằng chứng

  * refund\_amount (DECIMAL(12,0), NOT NULL): Số tiền yêu cầu hoàn

  * status (VARCHAR(30), NOT NULL, DEFAULT 'PENDING'): PENDING, ACCEPTED, REJECTED, ADMIN\_MEDIATING

  * admin\_note (TEXT, NULL): Ý kiến phán quyết của Admin sàn

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

 

 

## **5.4 Payment DB (payment\_db)**

 

* **Bảng wallets:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Ví

  * shop\_id (BIGINT, NOT NULL, UNIQUE): Logical FK tới shops.id

  * balance (DECIMAL(12,0), NOT NULL, DEFAULT 0): Số dư khả dụng có thể rút

  * frozen\_balance (DECIMAL(12,0), NOT NULL, DEFAULT 0): Số dư đóng băng chờ giải ngân hoặc chờ duyệt rút

* **Bảng escrow\_accounts:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Tài khoản Escrow

  * sub\_order\_id (BIGINT, NOT NULL, UNIQUE): Gắn với Sub-Order cụ thể

  * amount (DECIMAL(12,0), NOT NULL): Số tiền tạm giữ của đơn hàng

  * commission\_fee (DECIMAL(12,0), NOT NULL, DEFAULT 0): Phí hoa hồng sàn thu

  * status (VARCHAR(20), NOT NULL): HOLDING, RELEASED, REFUNDED

* **Bảng ledger\_entries (Sổ cái kép Double-Entry):**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Bút toán

  * transaction\_id (VARCHAR(100), NOT NULL): Mã giao dịch đối soát (Index)

  * transaction\_type (VARCHAR(50), NOT NULL): ESCROW\_HOLD, ESCROW\_RELEASE, COMMISSION\_FEE, WITHDRAWAL, REFUND

  * account\_debit (VARCHAR(50), NOT NULL): Tài khoản Ghi Nợ (VD: ESCROW\_HOLDING, SELLER\_WALLET)

  * account\_credit (VARCHAR(50), NOT NULL): Tài khoản Ghi Có (VD: SYSTEM\_ESCROW, PLATFORM\_REVENUE)

  * amount (DECIMAL(12,0), NOT NULL): Số tiền giao dịch

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

* **Bảng withdrawals:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Lệnh rút tiền

  * shop\_id (BIGINT, NOT NULL): Thuộc Shop nào

  * amount (DECIMAL(12,0), NOT NULL): Số tiền rút

  * bank\_code (VARCHAR(20), NOT NULL): Ngân hàng

  * bank\_account\_number (VARCHAR(50), NOT NULL): Số tài khoản nhận

  * bank\_account\_name (VARCHAR(100), NOT NULL): Tên chủ tài khoản

  * status (VARCHAR(30), NOT NULL, DEFAULT 'PENDING'): PENDING, APPROVED, REJECTED, COMPLETED

  * admin\_id (BIGINT, NULL): Admin phê duyệt

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

  * processed\_at (TIMESTAMP, NULL): Thời gian tất toán chuyển khoản

 

 

## **5.5 Shipping DB (shipping\_db)**

 

* **Bảng shipments:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Vận đơn

  * sub\_order\_id (BIGINT, NOT NULL, UNIQUE): Gắn với Sub-Order

  * tracking\_code (VARCHAR(50), NOT NULL, UNIQUE): Mã vận đơn từ GHN/GHTK

  * carrier\_name (VARCHAR(50), NOT NULL): GHN, GHTK, MOCK\_CARRIER

  * shipping\_fee (DECIMAL(12,0), NOT NULL, DEFAULT 0): Cước phí vận chuyển thực tế

  * shipping\_status (VARCHAR(50), NOT NULL): READY\_TO\_PICK, PICKED, DELIVERING, DELIVERED, DELIVERY\_FAILED, RETURNED

  * updated\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP)

* **Bảng shipping\_webhooks\_log:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Log Webhook

  * event\_id (VARCHAR(100), UNIQUE, NOT NULL): Khóa chống trùng lặp Idempotency

  * carrier\_name (VARCHAR(50), NOT NULL): Tên hãng gửi webhook

  * payload (TEXT, NOT NULL): Dữ liệu JSON webhook nhận được

  * processed\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

 

 

## **5.6 Notification DB (notification\_db)**

 

* **Bảng notifications:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID thông báo

  * user\_id (BIGINT, NOT NULL): Logical FK tới users.id

  * title (VARCHAR(200), NOT NULL): Tiêu đề thông báo

  * content (TEXT, NOT NULL): Nội dung thông báo

  * is\_read (BOOLEAN, DEFAULT FALSE)

  * created\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

* **Bảng email\_logs:**

  * id (BIGINT, PK, AUTO\_INCREMENT): ID Log Email

  * recipient\_email (VARCHAR(100), NOT NULL): Email người nhận

  * subject (VARCHAR(255), NOT NULL): Tiêu đề thư

  * status (VARCHAR(20), NOT NULL): SUCCESS, FAILED

  * sent\_at (TIMESTAMP, DEFAULT CURRENT\_TIMESTAMP)

 

 

# **6\. Kế hoạch Phát triển & Phân công Công việc (Phương pháp MoSCoW)**

 

## **6.1 Phân cấp Tính năng theo MoSCoW**

 

* **Must Have (Core MVP Bắt buộc hoàn thành):**

  * 1 API Gateway \+ 6 Backend Services kết nối 6 DB MySQL độc lập.

  * Phân quyền RBAC 3 vai trò: ROLE\_BUYER, ROLE\_SELLER, ROLE\_ADMIN.

  * Quản lý Gian hàng (Shop Onboarding & Approval).

  * Catalog CRUD Sản phẩm, Biến thể SKU, upload ảnh MinIO S3, Redis Cache.

  * Giỏ hàng Redis gom nhóm theo Shop, tách Master Order ![][image1] Sub-Orders theo Shop.

  * Saga Orchestrator kiểm tra giữ tồn kho có thời hạn (15 phút) qua RabbitMQ kèm cơ chế DLQ (order.dlq).

  * Thanh toán VNPay Sandbox (Tạo URL & Xử lý IPN Webhook).

  * Quản lý Escrow tạm giữ & Ví Seller theo mô hình Sổ cái kép Double-Entry.

  * Shipping Webhook xử lý Idempotency bằng event\_id.

  * CI/CD GitHub Actions Rolling Update tự động lên VPS.

* **Should Have (Ưu tiên cao):**

  * Áp dụng Voucher Kép (Sàn & Shop).

  * Đổi trả / Hoàn tiền một phần (refund\_requests) trong 3 ngày.

  * Tác vụ định kỳ ShedLock: Tự động hủy đơn quá hạn 15 phút, tự động giải phóng Escrow sau 3 ngày.

  * Spring AI Chatbot tư vấn sản phẩm \+ Tool sinh SEO description.

  * Giám sát Metrics với Prometheus/Grafana, Tracing với Zipkin.

* **Could Have / Won't Have (Giai đoạn mở rộng tiếp theo):**

  * Hệ thống gợi ý AI Recommendation Engine nâng cao.

  * Livestream Bán hàng & Flash Sale Engine chịu tải 10.000 RPS.

 

 

## **6.2 Phân công Trách nhiệm Thành viên**

 

| Thành viên | Trách nhiệm chính | Chi tiết công việc triển khai |
| :---- | :---- | :---- |
| **Lê Đức Anh** | DevOps & Infrastructure, Identity & Shop, API Gateway, AI Assistant | • Khởi tạo Repo, cấu hình Docker Compose toàn bộ hạ tầng (6 MySQL DBs, Redis, RabbitMQ, MinIO, Zipkin, Prometheus, Grafana).\<br\>• Phát triển API Gateway: Dual Auth (HttpOnly Cookie & Bearer Header), RBAC Authorization Filter, Rate Limiting ưu tiên CF-Connecting-IP.\<br\>• Phát triển Identity Service: Auth REST API, Quản lý hồ sơ Shop (shops), KYC và phê duyệt Seller.\<br\>• Viết GitHub Actions CI/CD Script deploy tự động lên VPS.\<br\>• Tích hợp Spring AI Chatbot tư vấn sản phẩm và hỗ trợ viết bài SEO. |
| **Triệu Quốc Bình** | Frontend Layout, Catalog & Product Service, Storage MinIO | • Dựng Giao diện Frontend ReactJS \+ Tailwind CSS Responsive (Storefront, Seller Center, Admin Dashboard).\<br\>• Phát triển Catalog Service: CRUD Danh mục, Sản phẩm, Biến thể SKU, đánh giá sao sản phẩm (product\_reviews).\<br\>• Tích hợp Redis Caching cho Catalog Service tối ưu tốc độ đọc ![][image2].\<br\>• Cấu hình MinIO Object Storage Server & SDK Upload Image.\<br\>• Phát triển Notification Service: Lắng nghe Async Message từ RabbitMQ gửi Mail OTP và thông báo. |
| **Nguyễn Văn Kiên** | Order & Cart, Payment & Wallet, Shipping & Logistics, E2E Testing | • Xây dựng Giỏ hàng Redis Cart Server gom nhóm theo Shop \+ Logic Merge Giỏ hàng Guest-User.\<br\>• Phát triển Order Service: Luồng Checkout tách Master/Sub-Orders, Saga Orchestrator qua RabbitMQ kèm Dead Letter Queue (order.dlq), quản lý Voucher và Khiếu nại Hoàn tiền.\<br\>• Phát triển Payment Service: Cổng thanh toán VNPay, Escrow Account, Double-Entry Ledger, Ví Seller và Lệnh rút tiền.\<br\>• Phát triển Shipping Service: Idempotent Webhook xử lý mã vận đơn GHN/GHTK.\<br\>• Viết Kịch bản E2E Integration Testing kiểm thử toàn bộ luồng hệ thống. |

   
 

 

## **6.3 Lịch trình Triển khai Chi tiết (Timeline 7 ngày)**

 

* **Tuần 1: Khởi động Hạ tầng, Đồng bộ cơ sở dữ liệu & Kiến trúc cốt lõi**  
  * **Lê Đức Anh:** Cấu hình Docker Compose (6 MySQL DBs, Redis, RabbitMQ, MinIO, Zipkin), hoàn thiện API Gateway Skeleton (Dual Auth & Route), phát triển Identity Service và viết GitHub Actions CI/CD script.  
  * **Triệu Quốc Bình:** Dựng bộ khung Frontend ReactJS Storefront & Dashboard, phát triển Catalog Service (Danh mục, Sản phẩm, Biến thể SKU) và cấu hình MinIO S3 upload.  
  * **Nguyễn Văn Kiên:** Khởi tạo schema cơ sở dữ liệu cho 6 Services, xây dựng Redis Cart Server gom nhóm theo Shop, cấu hình RabbitMQ Exchanges/Queues/DLQ.  
* **Tuần 2: Tích hợp Gateway, Caching, Giao dịch Phân tán Saga & Thanh toán**  
  * **Lê Đức Anh:** Cấu hình Auth Filter, kiểm tra RBAC, Rate Limiting tại Gateway; thiết lập VPS Ubuntu, Nginx Reverse Proxy và SSL.  
  * **Triệu Quốc Bình:** Tích hợp Redis Caching cho Catalog Service, hoàn thiện giao diện danh sách/chi tiết sản phẩm và Notification Service (gửi email OTP qua RabbitMQ).  
  * **Nguyễn Văn Kiên:** Thiết lập kịch bản Saga Orchestrator cho Order Service (giữ kho 15 phút), tích hợp cổng thanh toán VNPay Sandbox, xây dựng cơ chế Escrow Double-Entry Ledger và Shipping Webhook Idempotency.  
* **Tuần 3: Trợ lý AI, Giám sát, Kiểm thử E2E & Bàn giao**  
  * **Lê Đức Anh:** Tích hợp Spring AI Chatbot (tư vấn sản phẩm & sinh mô tả SEO), thiết lập hệ thống giám sát tập trung (Prometheus, Grafana, Loki, Zipkin).  
  * **Triệu Quốc Bình:** Hoàn thiện giao diện quản trị Admin Portal, tích hợp AI Chatbot Widget vào Storefront.  
  * **Nguyễn Văn Kiên & Cả nhóm:** Chạy kịch bản kiểm thử tích hợp đầu cuối (E2E Integration Testing), mô phỏng tải đồng thời (Load Test 1.000 RPS), khắc phục lỗi và bàn giao sản phẩm.  
* 

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAKCAYAAAC0VX7mAAAAgElEQVR4Xp3RoQ0AIQyFYRQbYFiBGUgYAgSjsAM7MQeKYFgAgXyXq6NnLv2Sml88UwWBWivWWjwTxcNf1lqeiHgwxojWGs/ywZf3HmOMq9FgSgkhBNE552CMuQeleu9QSl0PEg+ec2iM+5aftNY8EdHgnBOlFJ6JaDDnjL03z+QB+Yre++dAM3kAAAAASUVORK5CYII=>

[image2]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAE0AAAAOCAYAAABqxZFyAAADLElEQVR4Xr2XP0iyQRzHXSIHoQYj0GiQIqKIwqKlIQiDhrZCbIqiP0MNIjQFDbU4tASVkVsR4moKopNITlJj0NRS1BCIQ2338v297z3c/Z57LHnTD6h3n/s999zze567e3SJFpPP54XL5RIHBwe8yWJkZIQ+e3t7FBuNRnkIgbb5+Xmxu7tL5fPzcx7SFlxc/Ba4KHwWFhYaJm1qakrMzs5qDvE3Nzc2h6Ry9/j4qLl20HTSTk9PRSaT4bohjZKGtvf3d80hkfAqvC5dV1cX1y3HPhIHNjc3aZDNJgw4JQ3Ty5SM7e1tzYfDYWPc8PCw0beab8+4uroqfD6feH5+5k0/xilpKysrxos+Pj7WbpDf7zfGLS4ukq/X65bD2jgzM2PVsf719vaKnp4eyz08PNCNQL8XFxeW57y9vVFfbrebziWxj+QfWGf6+/vFy8sLb2oap6SNj48bk5FKpchfXl5SHWVTnHwin56eqC5j8OvxeLRj5LkwYxKJhOXhrq+vrboEN0I9HjcQfQBtJK+vr/RUTUxMiM/PT7Xpv3BKWkdHhzEZxWKR/OHhIdWdkra/v0++VCpRfXl5mX5N8Tg/3NbWlubh8NRx8GSqY0bc0dHR3zK+yuUyybm5OSvoN3FKGgbGLw5cXV2RT6fTVDclAayvr5PnGwkcnlaV0dFRWx8fHx/kbm9vNQ/kOaenp2k6a234whQMBAJtT9rY2JjtQkA8HievTjtTnHydUcEaxR2AGxoa0pzc3EzIfuQHS5VEO+Lr60sEg8G2Tc9YLGYc9MbGhuaxuJviBgcHbR5j5w7AnZ2d2ZwpViWbzQqv16vFOR4RCoVavhEA06D5BiGXDw6cuitK19fXpzk53TlweO+UZVAoFKhseuG2yoo3gkWyVa8cYHJy0lrAJYg/OTmxOd4HnNwEVJdMJjU3MDBgS5rcGEClUhFLS0tUzuVytlj8rWsqaZKdnR068Kcvt7hTiDd9OHBra2vUd3d3Ny0RJmTi8BcL5UgkorXf39879o8FnQOPcWJKq2A56OzstK4B73oq9jN8AwbN7+RvUKvVxN3dHdc2sFNWq1Wu28off+BIs9auBooAAAAASUVORK5CYII=>

[image3]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAAAOCAYAAACvtwvjAAADZ0lEQVR4Xs2YXyi7URjH3zsSK0VWSmlbTUrUSopyhYjsetIuFiVuXEwpUYT8ae7kAjdyYUnSaDezVqvdKJG4IpFxoxC35/d7Hj3n957z/hnvNn7fOpzn8zzn7HmfnZ1zNoVJOj4+ltGPa21tjSmKYpoL+Ht6etjIyAj2V1dX5RAU+LxeLwsEAhnnzKcUwfibCLTfEr3+6OioaVHAFwwGNezq6krD1HOA/7eeT1Noj8ejRjmRz+eTkammpqY0RSKdn5/rFguY3W7ndiQSMYxra2uTcd7FM7m5ucEkwuGw2m9ZLy8vrK6uztIbZ1boiooK3QLW1NQIvKysTDcOmB7Pt5TW1lZGDRKgvlVdXl7iyoJ90arMCm1UKMqfZBRXUlIicFhg5eXlrLOzkzPYz4uLi1ltbS17eHhAFo1GWW9vL6usrMQzxEhPT0+submZFRYW4hlCUuBhoFFiZH9XR0dHOH5wcFB2fVvZFPrj4wNtozi32615Q+g/FEbtq6+vR7ujo4MdHh5yDmx/f5/bpLe3N2H8wcEBzgHiFAKsrOT19XUcOzExIbssK5tCJ5NJtI3iWlpaOE+lUmxhYQH7evF+vx/Z+Pi4wIEtLi4KjPjk5KRgz8zMfPbVcGtriwdl0vT0NI5ZWVmRXVkLkrVa6MfHR7SN4hwOB+fq+YHt7u5ym5g8x/PzMzLYSmRRfGNjIzs9PRV98Gd5eVkzYSbBx+J/LDTJKK6oqEjDx8bGNAwErL29XWADAwO6saBQKMRfF1pVVRX34QjYuI0GZxIVPJdbh1mhjW4dLpfrS4XW43rs9vYWWSKREDiw6upqgcmCq6V868EegKampn9QJ8FMisfjOC4Xh6FZoWOxmG5+wGw2G7dnZ2cN47q7uzWsr69PYLQ/q3V9fY1se3sbbfLDXg/9oaEhdbh+off29hD09/djklZ1dnaGV6hsrndmhQaBT33oEIMHltnOzg6339/fkb2+vqqiPuMuLi40TC405QXa2NjgOdBPBnBVJA0PD7PS0lJu4yjYvOEuCU2dWDa6v79nTqfzW19Y6OH0miwq9ubmJvbn5ubkEJZOp9E3Pz/Pi7G0tCTE0L4qCxj8jiILeENDAzaZFxQU4FkBCw3u4oJfsPKkrq4uGeVE8GXi5ORExhrd3d1pVuxP6w8vTrd7DaSkVwAAAABJRU5ErkJggg==>

[image4]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAAAOCAYAAACvtwvjAAADyUlEQVR4XsWYTSg2URTHZ0dCKaKUCEVKlEJRVoiINclCSJQVpWRByPdOFtjIgpB8ZYOUsqBEPlYk8lUUYnvf9390bvfemfF4Hjz+NZ57fufcmTNnZs7cYQlDm5ubJvKbTk5OhGVZIj8/X2RlZdG4o6PDDCPBV1paKpqammg8OjpqhpDgKy8vFzU1NTT+q/OzNON/Itj+Sjj24+OjtAcGBoj19PQoUR9xLS0tNnZ2dmZjamHh/6vzsxU6IyNDRT+iiooKEznK6UKb7OjoyBYDgUVFRUl7ZWXFNQ5PjL8lM7m4uKAkZmdnVb/Pen5+FqmpqV5duMzMTDE0NCTtvb09yik7O1uyyMhIxwImJydrPDw83DHOvHD+kpWXlyd4QwI89lWnp6d0Z6Evflfx8fG2orgVivNnucWFhIRoHDdYRESEKCoqkgz9PDg4WKSkpIibmxti6+vroqysTERHR4uxsTEZa+r+/l7k5OSIwMBAeoewLPQwbJwY295qbW2N5tfV1Zkur3V4eCiqq6tpf/Pz85rPrYBc6Pf3d7Ld4pKSkmwXhH9RGNWXlpZGdmFhoVhdXZUcbHFxUdqs19dXbf7S0hLtA5IUAb7cyePj4zS3vb3ddPkkvtC1tbW038nJSc3vVkAu9M7ODtlucbm5uZLv7u6Kvr4+GjvF88Vua2vTOFh/f7/GmKurJNhdXV0fYxVOTU3JIE/q7OykOSMjI6brx3RwcEDHQNFZTgWBuNB3d3dku8Wp7Uh9csHm5uakzczcx9PTEzG0ElMcj3cNctd8+DM4OGjboSfhsfjtQkPmyZo266s9OigoyMZbW1ttDAIrKCjQGD9pThoeHpbHxRYTEyN9NAON222yJ3HBv9s68MhhP8vLyxrnpPnuc1t1JCYmfqnQTtyJXV5eEtve3tY4WFxcnMZMYWlprnpoBKAuocyDfkVbW1s0z9eXIZ+s+Z4wi7CxseGYH1hoaKi0u7u7XeNKSkpsrLKyUmPcn1Wdn58Tm56eJpv96PUYNzQ0qOHafFnohYUFAlVVVZSkr8KKAUsob5d3zc3NtiIfHx9TblgjqwIzP83BcMImm5mZkfbb2xuxl5cXJeojDscymVlofuqgiYkJmQOWe+BYKrIaGxtFWFiYtGkWmjfWktjUxL6j6+trkZCQ4NUHC/pfbGysqK+vp96I5N2+4rjYWJVgbH6mQ7e3t+Tr7e2VxcBnvSruq6bA8H8UU+Dp6em0mTwgIIBuFtxoWItrfs36JRUXF5voUz08PFCP9CR8TOzv75vYpqurK9sd62/9Az6VlTd5uYBQAAAAAElFTkSuQmCC>

[image5]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAAAOCAYAAACvtwvjAAADp0lEQVR4Xs2YTShtURTH74yEUkQpJRQpUUpEGSEixiQDUWJgQikZ+MxHzGSAiQxIkq9MkFImJBITJPIxUQjD/fqv99Z+Z6+zz+NeX+9f293rt9Y+Z+11z917Hz4ltLGxIdG3yefzUYuNjVU5OTkqNDSU7IWFBRlKvKysTDU1NVF/dHRUhpDgq6ioULW1tdT/qfn5DOPPRH9KfH9u4eHh6vX1VYaRr6WlxcVOTk5czFlY+H9qfq5CZ2ZmOtGnqLKyUiKr3lOEw8NDaxxYTEyMtpeXlz3jCgoKJP5y6UzOz88pidnZWac/YD08PKi0tDS/vjhbYaSio6OtcSkpKQaPjIy0xoHZ+FfLl5+fr7ghAe4HquPjY3qysC76Ky4AvqS9vT3h/S2vQnH+LK+4sLAwg+MBi4qKUsXFxZphPcf+kJqaqq6vr4mtra2p8vJy2j/GxsZ0rNTd3Z3Kzc1VwcHBtIewfFjD0Dgxtv3V6uoqja+vr5eudwvjMWlMiHPCZidjbAXkQr+8vJDtFZecnOz6QvgThXH60tPTyS4qKlIrKyuag9k26KenJ2P84uIiXQPSFAGBPMnj4+M0tr29Xbr8Fq5zdHTkYl1dXYZtKyAXent7m2yvuLy8PM13dnZUf38/9W3xNTU1xNra2gwONjAwYDDmHR0dhs25G4WemprSQW+ps7OTxoyMjEjXp0oWQNosLvTt7S3ZXnEJCQmaO3+5YHNzc9pmJq9xf39PDEuJFMdnZWWp/f1904c/Q0NDrgu+Jfws/sdCs7ziQkJCXLy1tdXFILDCwkKD1dXVWWOh4eFhfV+0uLg47aMRWLi9Br8lLvhHlw6eAJJ1ShbM69SRlJT0rkLbuI1dXFwQ29raMjhYfHy8waRwtJSnHuoBZGdn/4WWBN/S5uYmjQt0M+zu7qbx2FCckkVYX1+35geGFxxWT0+PZ1xpaamLVVVVGYzXZ6fOzs6ITU9Pk81+rPXoNzQ0OMPthZ6fnydQXV1NSQaqg4MDOkJ95HjHWlpaIoZfjVNgzk2HGSYs2czMjLafn5+JPT4+OqJ+x9k2YZkP7slsYmJC54DjHjiOiqzGxkYVERGhbRqFxRvHKjRnYh/R1dWVSkxM9OuFpbm5mRLG04T/daDvddTkYk9OTlK/t7dXhqibmxvy9fX16WIMDg4aMbyuSoHJoyXzjIwMapIHBQXRXoEHDWdxw29YX6SSkhKJ/qnT01OJrMLLxO7ursQuXV5eup7Y79YvqcqaVv/5C0UAAAAASUVORK5CYII=>