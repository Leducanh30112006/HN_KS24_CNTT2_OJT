# BÁO CÁO KIỂM THỬ GIAO DIỆN & TÍCH HỢP FRONTEND (QA FRONTEND REPORT)

**Người phụ trách:** Triệu Quốc Bình (Frontend & UI/UX Specialist)  
**Ngày thực hiện:** 15/11/2026  
**Phân hệ:** QA Frontend & End-to-End Integration  

---

## 1. Mục tiêu kiểm thử
Xác minh giao diện người dùng, tính tương thích responsive trên các thiết bị (Desktop, Tablet, Mobile), tính chính xác của các luồng nghiệp vụ trên frontend:
1. **Luồng Người mua (Buyer)**:
   - Duyệt danh mục đa cấp, tìm kiếm từ khóa, lọc theo khoảng giá, đánh giá sao, sắp xếp giá/mới nhất.
   - Chi tiết sản phẩm với chọn biến thể (Màu sắc, Dung lượng/Kích thước), hiển thị tồn kho và giá tương ứng.
   - Giỏ hàng chuẩn Shopee: Chọn tất cả, chọn theo shop, xóa sản phẩm, kéo/thả Widget AI Bot.
   - Áp dụng mã giảm giá 2 tầng: Voucher Sàn QuickShop và Voucher riêng của Shop.
   - Thanh toán đa phương thức: COD, Cổng thanh toán trực tuyến VNPay Sandbox, Ví điện tử Escrow.
2. **Luồng Người bán (Seller Center)**:
   - Đăng ký hồ sơ gian hàng KYC: Tên shop, MST, CCCD, kho lấy hàng, tài khoản ngân hàng.
   - Quản lý danh mục & đăng tải sản phẩm đa biến thể với hình ảnh.
   - Quản lý đơn hàng con (Sub-orders), cập nhật trạng thái đơn (Chuẩn bị hàng, Đã giao cho đơn vị vận chuyển).
   - Yêu cầu rút tiền doanh thu về ngân hàng.
3. **Luồng Quản trị viên (Admin Portal)**:
   - Kiểm duyệt sản phẩm đăng bán (Duyệt / Từ chối kèm lý do phản hồi cho Seller).
   - Quản lý và kích hoạt/khóa tài khoản Người bán và Người mua.
   - Quản lý voucher sàn toàn hệ thống (phát hành mã, số lượng giới hạn, mức giảm tối đa).
   - Quản lý cây danh mục sản phẩm (thêm danh mục cha/con, cập nhật slug, icon).
   - Giám sát GMV & Doanh thu hoa hồng sàn (Commission 5%), tài khoản ký quỹ (Escrow Holding).
   - Quản lý & phê duyệt yêu cầu rút tiền của Người bán.
   - Nhật ký kiểm toán hệ thống (Audit Logs) hỗ trợ lọc theo hành động và tìm kiếm.
   - Giám sát trạng thái hoạt động của 6 Microservices backend qua cổng Gateway 8080.

---

## 2. Bảng kết quả kiểm thử giao diện & tích hợp (Test Matrix)

| STT | Luồng kiểm thử | Thiết bị thử nghiệm | Trạng thái | Ghi chú & Fixes |
| :--- | :--- | :--- | :--- | :--- |
| **TC-FE-01** | Xem Catalog, tìm kiếm, lọc giá & phân trang | Desktop 1920x1080 & iPhone 14 Pro | **PASSED** | Hiển thị mượt mà, phân trang 12 item/trang |
| **TC-FE-02** | Xem chi tiết sản phẩm, chọn biến thể | Desktop & iPad Air | **PASSED** | Giá và tồn kho cập nhật tức thì theo SKU |
| **TC-FE-03** | Thêm giỏ hàng, tick chọn sản phẩm thanh toán | Desktop & Android Mobile | **PASSED** | Đúng phong cách Shopee, tổng tiền realtime |
| **TC-FE-04** | Áp dụng Voucher Sàn + Voucher Shop | Chrome & Safari Mobile | **PASSED** | Hỗ trợ nhập mã hoặc click chọn mã gợi ý |
| **TC-FE-05** | Thanh toán Checkout & chọn đối tác GHN/GHTK | Desktop & Mobile | **PASSED** | Tự động tính cước vận chuyển chuẩn xác |
| **TC-FE-06** | Phân quyền truy cập Kênh Người Bán (ROLE_SELLER) | Desktop & Mobile | **PASSED** | Chặn người chưa đăng nhập / Buyer thường |
| **TC-FE-07** | Phân quyền Cổng Quản Trị (ROLE_ADMIN) | Desktop & Mobile | **PASSED** | Chỉ tài khoản có vai trò ADMIN mới vào được |
| **TC-FE-08** | Màn hình GMV, Phí sàn & Sổ cái kép | Desktop 1440x900 | **PASSED** | Bảng số liệu rõ ràng, duyệt rút tiền ngay |
| **TC-FE-09** | Màn hình Audit Log có tìm kiếm & bộ lọc | Desktop 1440x900 | **PASSED** | Lọc theo Phê duyệt, Từ chối, Webhook |
| **TC-FE-10** | Widget Chatbot AI kéo thả vị trí linh hoạt | Desktop & Mobile | **PASSED** | Kéo thả không che khuất footer hay nút bấm |

---

## 3. Đánh giá chất lượng Frontend
- Build hoàn toàn sạch sẽ (`vite build`), không có cảnh báo cú pháp JSX.
- Thời gian tải trang ban đầu (First Contentful Paint) < 1.2s.
- Hỗ trợ tốt cơ chế tự động Fallback sang dữ liệu mẫu phong phú khi chạy offline hoặc backend đang khởi động.
