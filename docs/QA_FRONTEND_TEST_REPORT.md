# BÁO CÁO KIỂM THỬ GIAO DIỆN & TÍCH HỢP FRONTEND (QA FRONTEND REPORT)

**Người phụ trách:** Triệu Quốc Bình (Frontend & UI/UX Specialist)  
**Ngày thực hiện:** **20/11/2026** (Nghiệm thu Sprint 4)  
**Phân hệ:** QA Frontend & End-to-End Integration  

---

## 1. Mục tiêu kiểm thử
Xác minh giao diện người dùng, tính tương thích responsive trên các thiết bị (Desktop, Laptop, Tablet, Mobile), tính chính xác của các luồng nghiệp vụ trên frontend qua 4 phân hệ:
1. **Luồng Người mua (Buyer Portal)**:
   - Duyệt catalog flagship, Bento Hero Grid, modal bộ lọc căn giữa màn hình, tìm kiếm tức thì.
   - Chi tiết sản phẩm với chọn biến thể (Màu sắc, Dung lượng/Kích thước), hiển thị tồn kho và giá tương ứng theo SKU.
   - Bản đồ số Leaflet OpenStreetMap chọn địa chỉ giao hàng và lấy tọa độ GPS Nominatim.
   - Giỏ hàng Shopee-style: Tick chọn theo shop, đồng bộ realtime, mã giảm giá kép (Sàn + Shop).
   - Thanh toán đa phương thức: COD, Cổng thanh toán VNPay Sandbox, và quét mã PayOS VietQR EMVCo.
   - Hệ thống tin nhắn Full-page Messenger trò chuyện với shop, nhúng thẻ sản phẩm thông minh.
2. **Luồng Người bán (Seller Center)**:
   - Đăng ký hồ sơ gian hàng KYC và upload ảnh giấy tờ lên MinIO.
   - Soạn thảo mô tả sản phẩm bằng bộ công cụ phong phú CKEditor 5, hỗ trợ chèn ảnh trực tiếp.
   - Quản lý đơn hàng con (Sub-orders), bảng thống kê doanh thu chi tiết theo từng mã sản phẩm SKU.
   - Yêu cầu rút tiền doanh thu về ngân hàng.
3. **Luồng Quản trị viên (Admin Portal)**:
   - Phê duyệt gian hàng KYC và kiểm duyệt sản phẩm đăng bán của Seller.
   - Cổng quản trị Flash Sale đa chế độ (Manual & AI Auto-Approve) và đồng hồ đếm ngược.
   - Quản lý voucher sàn, quản lý tài khoản người dùng và khóa tài khoản vi phạm.
   - Giám sát GMV & Doanh thu hoa hồng sàn (Commission 5%), tài khoản ký quỹ (Escrow Holding) và phê duyệt rút tiền.
4. **Luồng Nhân viên Giao Hàng (Shipper Portal)**:
   - Đăng ký & đăng nhập bưu tá với giao diện tối ưu hóa cho điện thoại di động.
   - Tiếp nhận đơn lấy hàng (`READY_TO_PICK`), cập nhật đang giao (`DELIVERING`) và bấm hoàn tất đơn hàng.
5. **Trang Giới thiệu 3D Scrollytelling (IntroPage)**:
   - Trải nghiệm chuyển động 3D WebGL/Three.js, cuộn mượt Lenis và hiệu ứng GSAP không giật lag.

---

## 2. Bảng kết quả kiểm thử giao diện & tích hợp (Test Matrix)

| STT | Luồng kiểm thử | Thiết bị thử nghiệm | Trạng thái | Ghi chú & Fixes |
| :--- | :--- | :--- | :--- | :--- |
| **TC-FE-01** | Xem Catalog, Bento Hero, Modal lọc giá & phân trang | Desktop 1920x1080 & iPhone 14 Pro | **PASSED** | Modal căn giữa mượt mà, phân trang 12 item/trang |
| **TC-FE-02** | Xem chi tiết sản phẩm, chọn biến thể SKU | Desktop & iPad Air | **PASSED** | Giá và tồn kho cập nhật tức thì theo SKU |
| **TC-FE-03** | Thêm giỏ hàng, tick chọn sản phẩm theo shop | Desktop & Android Mobile | **PASSED** | Đúng phong cách Shopee, tổng tiền realtime |
| **TC-FE-04** | Áp dụng Voucher Sàn + Voucher Shop | Chrome & Safari Mobile | **PASSED** | Hỗ trợ nhập mã hoặc click chọn mã gợi ý |
| **TC-FE-05** | Bản đồ số Leaflet OSM chọn địa chỉ nhận hàng | Desktop & Mobile | **PASSED** | Chấm điểm GPS chính xác, không lỗi CSP Nginx |
| **TC-FE-06** | Quét mã thanh toán VietQR PayOS EMVCo | Desktop & Mobile | **PASSED** | Sinh mã QR động sắc nét, đếm ngược tự động chuyển trang khi hoàn tất |
| **TC-FE-07** | Đăng nhập một chạm Google OAuth & OTP Email | Chrome, Safari, Edge | **PASSED** | Tự động điền email thật và chuyển hướng an toàn |
| **TC-FE-08** | Giao diện Chat Full-page Messenger & Thẻ sản phẩm | Desktop & Mobile | **PASSED** | Chat tức thì giữa Buyer và Seller, gửi thẻ sản phẩm mượt mà |
| **TC-FE-09** | Kênh Shipper Portal giao vận đơn hàng | iPhone 14 Pro & Samsung Galaxy | **PASSED** | Thao tác 1 chạm nhận đơn và xác nhận đã giao |
| **TC-FE-10** | Quản trị Flash Sale (Manual & AI Auto-Approve) | Desktop 1440x900 | **PASSED** | Bật tắt AI duyệt nhanh, đồng hồ đếm ngược đồng bộ |
| **TC-FE-11** | Soạn thảo CKEditor 5 tải ảnh MinIO nội tuyến | Desktop & Laptop | **PASSED** | Tải ảnh nhanh < 500ms, format phong phú |
| **TC-FE-12** | Trang Giới thiệu 3D Scrollytelling Showcase | Màn hình Retina & Full HD | **PASSED** | Khung hình ổn định 60fps, cuộn mượt Lenis |
| **TC-FE-13** | Phân quyền Kênh Người Bán (ROLE_SELLER) | Desktop & Mobile | **PASSED** | Chặn người chưa đăng nhập / Buyer thường |
| **TC-FE-14** | Phân quyền Cổng Quản Trị (ROLE_ADMIN) | Desktop & Mobile | **PASSED** | Chỉ tài khoản có vai trò ADMIN mới vào được |
| **TC-FE-15** | Màn hình GMV, Phí sàn & Sổ cái kép | Desktop 1440x900 | **PASSED** | Bảng số liệu rõ ràng, duyệt rút tiền ngay |
| **TC-FE-16** | Bảng thống kê doanh thu theo từng mã SKU | Desktop & iPad | **PASSED** | Báo cáo chi tiết từng biến thể sản phẩm |
| **TC-FE-17** | Lịch sử đơn hàng tách thẻ Sub-Orders theo Shop | Desktop & Mobile | **PASSED** | Hiển thị rõ logo shop, mã vận đơn và tình trạng gói hàng |
| **TC-FE-18** | Widget Chatbot AI kéo thả vị trí linh hoạt | Desktop & Mobile | **PASSED** | Kéo thả không che khuất footer hay nút bấm |

---

## 3. Đánh giá chất lượng Frontend
- Build hoàn toàn sạch sẽ (`vite build`), không phát sinh lỗi cú pháp hay cảnh báo TDZ.
- Thời gian tải trang ban đầu (First Contentful Paint) < 1.0s.
- Hỗ trợ tốt cơ chế tự động Fallback sang dữ liệu mẫu phong phú khi chạy offline hoặc backend đang khởi động.
- Đạt 100% tiêu chí nghiệm thu giao diện người dùng theo chuẩn thiết kế Impeccable.
