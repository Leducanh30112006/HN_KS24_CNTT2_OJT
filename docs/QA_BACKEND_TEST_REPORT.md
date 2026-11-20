# BÁO CÁO KIỂM THỬ TÍCH HỢP HỆ THỐNG BACKEND (QA BACKEND REPORT)

**Người phụ trách:** Nguyễn Văn Kiên (Backend & Saga & Shipping/Payment)  
**Ngày thực hiện:** **20/11/2026** (Kết thúc Sprint 4)  
**Phân hệ:** QA Backend & Microservices Integration  

---

## 1. Mục tiêu kiểm thử
Xác minh tính đúng đắn và độ bền vững (fault-tolerance) của toàn bộ các luồng nghiệp vụ backend:
1. **Quy trình Checkout & Giỏ hàng**: Kiểm tra tính toán tổng tiền, áp dụng voucher sàn (QuickShop) và voucher shop song song.
2. **Saga Orchestration & Compensation**: Kiểm tra luồng Saga khi thanh toán thành công và luồng bù trừ (compensation rollback stock, voucher, order) khi thanh toán thất bại.
3. **Thanh toán VNPay & PayOS VietQR & COD**: Xử lý callback thanh toán, sinh mã QR VietQR EMVCo, xác nhận giao dịch và tạo đơn hàng.
4. **Hệ thống Ký quỹ (Escrow) & Sổ cái kế toán kép (Ledger)**: Đóng băng tiền đơn hàng, ghi nhận ghi nợ/ghi có (Debit/Credit), tự động giải phóng tiền cho người bán sau 72h hoàn tất đơn, trích hoa hồng sàn (Commission 5%).
5. **Shipping & Webhook Idempotency & Kênh Shipper**: Tích hợp tính phí giao hàng (GHN, GHTK, Mock Simulator), tạo mã vận đơn tự động, xử lý webhook bất đồng bộ đảm bảo không bị xử lý trùng lặp dựa trên `event_id`, và API tiếp nhận/giao hàng dành cho Shipper.
6. **Scheduled Tasks & Dead Letter Queue (DLQ)**: Quét đơn hàng quá hạn thanh toán sau TTL (Order Expiration Scheduler) và xử lý thông điệp lỗi tự động qua RabbitMQ DLQ.
7. **Định danh nâng cao & Bản đồ**: Xác thực Google OAuth 2.0, OTP qua Email với Redis TTL, và Nginx Reverse Proxy Tile/Nominatim geocoding.

---

## 2. Danh mục kịch bản kiểm thử (Test Cases)

| Mã test | Hạng mục kiểm thử | Mô tả kịch bản | Kết quả mong đợi | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| **TC-BE-01** | Checkout API | Người mua checkout 2 sản phẩm từ 2 shop khác nhau, áp voucher shop + sàn | Tính đúng subtotal, giảm giá, phí ship từng shop, tạo 1 Master Order và 2 Sub-Orders | **PASSED** |
| **TC-BE-02** | Saga Success | Đặt hàng thành công, trừ kho -> khóa voucher -> khởi tạo thanh toán -> xác nhận đơn | Tất cả sub-orders chuyển trạng thái `CONFIRMED`, sự kiện phát đi đầy đủ | **PASSED** |
| **TC-BE-03** | Saga Rollback | Thanh toán thất bại hoặc kho không đủ | Saga kích hoạt bù trừ: hoàn kho, mở lại voucher, cập nhật đơn `CANCELLED` | **PASSED** |
| **TC-BE-04** | Order Expiration | Đơn hàng chờ thanh toán quá hạn 15 phút | Cron job phát hiện và tự động chuyển sang trạng thái hủy, giải phóng hàng tồn | **PASSED** |
| **TC-BE-05** | RabbitMQ DLQ | Sự kiện đơn hàng gặp sự cố deserialization hoặc lỗi hệ thống | Tin nhắn chuyển sang Dead Letter Queue, endpoint DLQ cho phép retry an toàn | **PASSED** |
| **TC-BE-06** | Shipping Calculator | Tính phí vận chuyển GHN/GHTK nội tỉnh và liên tỉnh, phụ thu cân nặng > 1000g, hỏa tốc | Giá cước chính xác theo công thức quy định | **PASSED** |
| **TC-BE-07** | Shipping Create | Tạo vận đơn giao hàng với mã định danh tự sinh (`GHN-XXXX`, `GHTK-XXXX`) | Trạng thái ban đầu `READY_TO_PICK`, liên kết đúng subOrderId | **PASSED** |
| **TC-BE-08** | Webhook Idempotent | Nhận cùng 1 webhook `event_id` 2 lần liên tiếp từ nhà vận chuyển | Lần 1: cập nhật trạng thái đơn; Lần 2: trả về HTTP 200 Idempotent, không ghi trùng | **PASSED** |
| **TC-BE-09** | Escrow Hold | Khách thanh toán đơn hàng thành công | Đóng băng tiền ký quỹ của SubOrder, ghi sổ cái `ESCROW_HOLD` (Debit/Credit) | **PASSED** |
| **TC-BE-10** | Escrow Release | Khách xác nhận đã nhận hàng (hoặc vận chuyển thành công) | Giải phóng số tiền thực nhận vào ví Shop, trích hoa hồng sàn, ghi sổ cái 2 bút toán | **PASSED** |
| **TC-BE-11** | Escrow Refund | Đơn hàng bị hủy/trả hàng | Hoàn tiền cho khách hàng, mở đóng băng ví shop, ghi sổ cái `ESCROW_REFUND` | **PASSED** |
| **TC-BE-12** | Shop Withdrawal | Người bán yêu cầu rút tiền về ngân hàng | Trừ số dư khả dụng, chuyển sang đóng băng chờ duyệt; Admin duyệt thì trừ hẳn | **PASSED** |
| **TC-BE-13** | PayOS VietQR | Tạo mã QR thanh toán động EMVCo cho đơn hàng đa shop | Trả về chuỗi VietQR chuẩn, mô tả đơn và webhook xác nhận tức thì | **PASSED** |
| **TC-BE-14** | Google OAuth & OTP | Đăng nhập tài khoản Google và xác thực OTP kích hoạt tài khoản qua Email | Tạo tài khoản an toàn, lưu mã OTP vào Redis TTL 5 phút, Rate Limit 60s | **PASSED** |
| **TC-BE-15** | Shipper APIs | Tiếp nhận đơn hàng (`READY_TO_PICK`), cập nhật đang giao (`DELIVERING`) và hoàn tất | Phân quyền `ROLE_SHIPPER` chuẩn, đồng bộ trạng thái đơn và vận đơn | **PASSED** |
| **TC-BE-16** | Flash Sale Manager | Tạo khung giờ Flash Sale, bật chế độ AI Auto-Approve duyệt sản phẩm | Giá khuyến mãi cập nhật chính xác, đếm ngược đồng bộ toàn hệ thống | **PASSED** |
| **TC-BE-17** | 72h Auto-Release | Đơn hàng hoàn tất giao sau 72 giờ | Scheduled Task tự động chuyển tiền ký quỹ vào số dư khả dụng ví người bán | **PASSED** |
| **TC-BE-18** | Geocoding Proxy | Lấy tọa độ GPS qua Nominatim qua Nginx reverse proxy | Chuyển đổi địa chỉ thành kinh độ/vĩ độ chính xác, không vi phạm CSP | **PASSED** |

---

## 3. Thống kê kết quả kiểm thử đơn vị & tích hợp
- **Order Service**: 100% test suites passed (CartController, OrderController, VoucherController, CheckoutService, SagaOrchestrator, OrderExpirationScheduler, OrderDlqListener).
- **Shipping Service**: 100% test suites passed (ShippingServiceTest, ShippingControllerTest, ShipperDeliveryTest).
- **Payment Service**: 100% test suites passed (PaymentServiceTest, PaymentControllerTest, PayOSWebhookTest, LedgerTest).
- **Catalog & Identity Service**: 100% test suites passed (ProductService, SkuService, GoogleAuthTest, OtpServiceTest, MinioStorageTest).
- **Notification AI Service**: 100% test suites passed (NotificationConsumerTest, GeminiServiceTest).

## 4. Kết luận
Toàn bộ 18 kịch bản kiểm thử tích hợp backend đáp ứng hoàn hảo yêu cầu thiết kế hệ thống thương mại điện tử phân tán (Microservices & Cloud-Native), bảo đảm tính toàn vẹn dữ liệu (Data Consistency), an toàn tài chính tuyệt đối và khả năng mở rộng cao.
