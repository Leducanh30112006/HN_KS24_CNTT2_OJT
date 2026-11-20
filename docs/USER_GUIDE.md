# TÀI LIỆU HƯỚNG DẪN SỬ DỤNG HỆ THỐNG QUICKSHOP (USER GUIDE)

**Dự án:** Sàn Thương Mại Điện Tử Đa Người Bán QuickShop (Marketplace Edition)  
**Phiên bản:** Release 1.0 (Production Final)  
**Tác giả:** Triệu Quốc Bình (Frontend Specialist & UI/UX Lead)  
**Ngày cập nhật:** **20/11/2026** (Kết thúc Sprint 4)  

---

## 1. Dành cho Người Mua (Buyer Guide)

### 1.1. Khám phá & Chọn mua sản phẩm
1. **Trải nghiệm Flagship:** Truy cập trang chủ với bố cục Bento Grid hiện đại hoặc khám phá trang tương tác 3D Scrollytelling tại `/intro`.
2. **Tìm kiếm & Bộ lọc:**
   - Tìm kiếm từ khóa sản phẩm trên thanh Header (hoặc nhận gợi ý thông minh từ AI).
   - Nhấn nút "Bộ lọc" để mở Modal Dialog căn giữa màn hình: Lọc theo danh mục, khoảng giá, và số sao đánh giá (1★ - 5★).
3. **Xem chi tiết & Bản đồ định vị:**
   - Chọn biến thể sản phẩm (Màu sắc, Dung lượng/Kích thước), giá và tồn kho SKU sẽ cập nhật tức thì.
   - Nhấn **"Thêm vào giỏ hàng"** hoặc **"Mua ngay"**.

### 1.2. Quản lý giỏ hàng & Thanh toán
1. **Giỏ hàng Shopee Style:** Truy cập `/cart` hoặc mở Drawer giỏ hàng nhanh. Tick chọn sản phẩm theo từng Shop hoặc nhấn **"Chọn tất cả"**.
2. **Áp dụng Mã giảm giá 2 tầng (Vouchers):**
   - **Voucher Sàn QuickShop:** Chọn mã freeship hoặc mã giảm giá phần trăm toàn sàn.
   - **Voucher Riêng của Shop:** Áp dụng ưu đãi từ chính gian hàng bán sản phẩm.
3. **Bản đồ chọn địa chỉ giao hàng:**
   - Khi điền địa chỉ giao hàng, nhấp vào bản đồ số tương tác **Leaflet OpenStreetMap** để ghim vị trí chính xác; hệ thống tự động điền tọa độ GPS qua công cụ Nominatim Geocoding.
4. **Phương thức thanh toán đa dạng:**
   - **COD:** Thanh toán tiền mặt khi nhận hàng.
   - **VNPay Sandbox:** Chuyển hướng tới cổng thanh toán VNPay.
   - **PayOS VietQR:** Sinh mã QR VietQR EMVCo động chứa số tiền và nội dung chuyển khoản chính xác, quét mã bằng ứng dụng ngân hàng và hệ thống tự động xác nhận sau khi nhận tiền.

### 1.3. Trò chuyện thời gian thực (Full-page Messenger)
- Nhấn biểu tượng Messenger ở góc dưới hoặc truy cập `/messenger` để chat trực tiếp với chủ gian hàng; tự động gửi kèm Thẻ sản phẩm thông minh đang xem để hỏi tư vấn.

---

## 2. Dành cho Người Bán (Seller Center Guide)

### 2.1. Đăng ký gian hàng & Phê duyệt KYC
1. Nhấp chọn "Kênh Người Bán" trên Header (hoặc đăng nhập tài khoản: `seller@techworld.vn` / `123456`).
2. Điền thông tin định danh: Tên shop, Mã số thuế, Số CCCD, Địa chỉ kho lấy hàng, Tài khoản ngân hàng thụ hưởng.
3. Chờ Quản trị viên duyệt hồ sơ KYC sang trạng thái `ACTIVE`.

### 2.2. Đăng tải sản phẩm với Trình soạn thảo phong phú & AI
1. Vào mục **"Quản lý sản phẩm"** -> Bấm **"Thêm sản phẩm mới"**.
2. **Bộ soạn thảo CKEditor 5:** Soạn thảo mô tả sản phẩm chi tiết, định dạng văn bản chuyên nghiệp và chèn ảnh nội tuyến trực tiếp lên kho lưu trữ MinIO.
3. **Sinh mô tả bằng Trí tuệ nhân tạo (AI SEO Generator):** Nhấn nút "Tạo mô tả bằng AI" để Gemini AI tự động tạo bài viết chuẩn SEO dựa trên thông số kỹ thuật.
4. Thiết lập ma trận biến thể SKU: Màu sắc, Kích cỡ, Giá niêm yết, Số lượng tồn kho và nhấn **"Gửi duyệt sản phẩm"**.

### 2.3. Quản lý đơn hàng & Thống kê tài chính
1. **Quản lý Sub-Orders:** Tiếp nhận đơn hàng mới theo shop, bấm **"Xác nhận đơn"** để chuẩn bị hàng cho Shipper lấy.
2. **Thống kê doanh thu theo từng mã SKU:** Xem bảng phân tích chi tiết doanh số từng sản phẩm để tối ưu kế hoạch nhập hàng.
3. **Rút tiền ví:** Tiền bán hàng sẽ tự động giải phóng từ tài khoản ký quỹ Escrow vào số dư khả dụng sau 72h đơn hoàn tất giao. Người bán có thể gửi yêu cầu rút tiền về ngân hàng bất kỳ lúc nào.

---

## 3. Dành cho Quản trị viên (Admin Portal Guide)

### 3.1. Truy cập Cổng Quản trị
- Đăng nhập với tài khoản Quản trị viên: `admin@quickshop.vn` / `admin123`.
- Truy cập vào mục **"Cổng Quản Trị Hệ Thống"**.

### 3.2. Các phân hệ quản trị chính
1. **Kiểm duyệt sản phẩm:** Duyệt hoặc từ chối sản phẩm đăng bán của Seller kèm lý do phản hồi rõ ràng.
2. **Phê duyệt gian hàng KYC:** Rà soát thông tin pháp lý của các Shop mới đăng ký và cấp quyền `ROLE_SELLER`.
3. **Quản trị Chiến dịch Flash Sale:**
   - Thiết lập khung giờ Flash Sale và mức chiết khấu sốc.
   - Lựa chọn 2 chế độ: Chọn sản phẩm thủ công (Manual) hoặc kích hoạt Trí tuệ nhân tạo (AI Mode) kèm công tắc **AI Auto-Approve** tự động duyệt sản phẩm đáp ứng tiêu chí.
4. **Giám sát Tài chính & Sổ cái kép:**
   - Theo dõi GMV toàn sàn, doanh thu hoa hồng 5%, tài khoản ký quỹ Escrow Holding.
   - Duyệt hoặc từ chối các yêu cầu rút tiền của Người bán.
   - Tra cứu Sổ cái kế toán kép (Double-entry Ledger) bảo đảm cân bằng thu chi.
5. **Giám sát Hệ thống:** Theo dõi uptime và trạng thái hoạt động của 7 Microservices backend qua Spring Actuator.

---

## 4. Dành cho Nhân viên Giao Hàng (Shipper Portal Guide)

### 4.1. Đăng nhập Kênh Giao Vận
- Truy cập vào mục **"Kênh Giao Hàng (Shipper)"** trên hệ thống.
- Đăng nhập với tài khoản bưu tá: `shipper@quickshop.vn` / `123456` (hoặc đăng ký tài khoản bưu tá mới).

### 4.2. Quy trình xử lý đơn giao hàng
1. **Tiếp nhận đơn hàng cần lấy (`READY_TO_PICK`):** Xem danh sách kiện hàng của các Shop đã chuẩn bị xong, nhấn nút **"Nhận đơn lấy hàng"**.
2. **Cập nhật đang giao (`DELIVERING`):** Khi đã nhận hàng từ kho người bán, nhấn **"Bắt đầu giao hàng"** để hệ thống gửi thông báo cho người mua.
3. **Xác nhận giao thành công (`DELIVERED`):** Khi người mua nhận hàng thành công (và thanh toán COD nếu có), nhấn **"Xác nhận đã giao thành công"**. Hệ thống sẽ tự động kích hoạt đếm ngược 72h giải phóng tiền ký quỹ Escrow cho gian hàng.
