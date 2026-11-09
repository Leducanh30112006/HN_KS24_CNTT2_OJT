# TÀI LIỆU HƯỚNG DẪN SỬ DỤNG HỆ THỐNG QUICKSHOP (USER GUIDE)

**Dự án:** Sàn Thương Mại Điện Tử Đa Người Bán QuickShop  
**Phiên bản:** Release 1.0  
**Tác giả:** Triệu Quốc Bình (Frontend Specialist)  
**Ngày cập nhật:** 16/11/2026  

---

## 1. Dành cho Người Mua (Buyer Guide)

### 1.1. Tìm kiếm và chọn mua sản phẩm
1. **Tìm kiếm:** Nhập từ khóa sản phẩm trên thanh tìm kiếm của Header (hoặc dùng gợi ý tìm kiếm thông minh).
2. **Bộ lọc:** Sử dụng thanh lọc bên trái để lọc theo danh mục, khoảng giá (Dưới 1 triệu, 1-5 triệu, 5-20 triệu, Trên 20 triệu) và đánh giá sao (1★ - 5★).
3. **Xem chi tiết:** Bấm vào thẻ sản phẩm để mở trang chi tiết, lựa chọn Màu sắc và Kích thước/Dung lượng mong muốn.
4. **Thêm vào giỏ hàng:** Nhấn **"Thêm vào giỏ hàng"** hoặc **"Mua ngay"**.

### 1.2. Quản lý giỏ hàng & Thanh toán
1. **Giỏ hàng Shopee Style:** Bấm vào biểu tượng Giỏ hàng trên thanh điều hướng hoặc truy cập trang `/cart`.
2. **Chọn sản phẩm thanh toán:** Tích chọn từng sản phẩm hoặc bấm **"Chọn tất cả"**. Chỉ những sản phẩm được tick mới được chuyển sang trang thanh toán.
3. **Áp dụng Mã giảm giá (Vouchers):**
   - **Voucher Sàn QuickShop:** Nhập mã (ví dụ `QUICKSHOP50K`, `FREESHIPMAX`) hoặc chọn nhanh từ danh sách mã ưu đãi có sẵn.
   - **Voucher Riêng của Shop:** Áp dụng ưu đãi từ chính gian hàng bán sản phẩm.
4. **Thanh toán:**
   - Điền thông tin giao hàng: Họ tên, Số điện thoại, Địa chỉ nhận hàng.
   - Chọn phương thức vận chuyển: Tiêu chuẩn (GHN/GHTK) hoặc Hỏa tốc 24h.
   - Chọn phương thức thanh toán: COD (Thanh toán khi nhận hàng), VNPay Sandbox, hoặc Ví ký quỹ Escrow.
   - Nhấn **"Đặt hàng"** để hoàn tất đơn mua.

---

## 2. Dành cho Người Bán (Seller Center Guide)

### 2.1. Đăng ký gian hàng & Phê duyệt KYC
1. Truy cập Kênh Người Bán từ thanh điều hướng trên cùng (cần tài khoản có vai trò `ROLE_SELLER` hoặc đăng nhập `seller@techworld.vn`).
2. Điền đầy đủ thông tin định danh: Mã số thuế, Số CCCD/Hộ chiếu, Địa chỉ kho lấy hàng, Thông tin tài khoản ngân hàng thụ hưởng.
3. Chờ Quản trị viên duyệt hồ sơ gian hàng sang trạng thái `ACTIVE`.

### 2.2. Đăng tải sản phẩm mới
1. Vào tab **"Quản lý sản phẩm"** -> Bấm **"Thêm sản phẩm mới"**.
2. Nhập Tên sản phẩm, Danh mục, Mô tả chi tiết, Đường dẫn ảnh chính.
3. Thiết lập các biến thể SKU: Màu sắc, Kích cỡ, Giá bán niêm yết, Số lượng tồn kho.
4. Nhấn **"Gửi duyệt sản phẩm"**. Sản phẩm sẽ vào hàng đợi kiểm duyệt của Admin.

### 2.3. Xử lý đơn hàng & Rút tiền doanh thu
1. **Đơn hàng:** Theo dõi các Sub-Orders phát sinh, bấm **"Xác nhận đơn"** và chuẩn bị bàn giao cho shipper.
2. **Rút tiền:** Vào tab **"Doanh thu & Ví tiền"** -> Nhập số tiền muốn rút về tài khoản ngân hàng -> Nhấn **"Gửi yêu cầu rút tiền"**.

---

## 3. Dành cho Quản trị viên (Admin Portal Guide)

### 3.1. Truy cập Cổng Quản trị
- Đăng nhập với tài khoản Quản trị viên (`admin@quickshop.vn` / mật khẩu quản trị).
- Truy cập vào mục **"Quản trị hệ thống (Admin Portal)"**.

### 3.2. Các phân hệ quản trị chính
1. **Duyệt sản phẩm:** Xem danh sách sản phẩm `PENDING`, duyệt phát hành lên sàn hoặc từ chối kèm lý do phản hồi rõ ràng.
2. **Quản lý Người bán:** Duyệt gian hàng mới đăng ký, xem chi tiết hồ sơ KYC (CCCD, Thuế, Kho, Ngân hàng), khóa/mở khóa gian hàng vi phạm.
3. **Quản lý Voucher Sàn:** Tạo các chiến dịch khuyến mãi, mã giảm giá toàn sàn, giới hạn số lượt và ngày hết hạn.
4. **Quản lý Danh mục:** Quản lý cây danh mục đa cấp, thêm danh mục mới, chỉnh sửa slug và icon hiển thị.
5. **GMV & Tài chính:**
   - Theo dõi chỉ số GMV toàn sàn, doanh thu hoa hồng 5%, số dư tiền ký quỹ Escrow đang bảo vệ giao dịch.
   - Quản lý danh sách yêu cầu rút tiền của Người bán: Nhấn **"Duyệt chi"** để xác nhận chuyển khoản ngân hàng hoặc **"Từ chối"** để hoàn lại tiền vào ví người bán.
   - Tra cứu sổ cái kế toán kép (Double-entry Ledger) ghi nhận đầy đủ bút toán Nợ/Có.
6. **Nhật ký kiểm toán (Audit Logs):** Tra cứu lịch sử thao tác của các thành viên quản trị, sự kiện webhook từ đối tác vận chuyển theo bộ lọc và từ khóa.
7. **Hạ tầng & Hệ thống:** Theo dõi tình trạng kết nối và uptime của 6 microservices (Gateway 8080, Identity 8081, Catalog 8082, Order 8083, Payment 8084, Shipping 8085, Notification AI 8086).
