# CHECKLIST PHÁT HÀNH HỆ THỐNG (RELEASE CHECKLIST)

**Dự án:** Sàn Thương Mại Điện Tử Đa Người Bán QuickShop (Marketplace Edition)  
**Phiên bản phát hành:** Release 1.0 (Production Final - Kết thúc Sprint 4)  
**Thời gian hoàn thành:** **20/11/2026**  
**Điều phối viên:** Lê Đức Anh  

---

## 1. Tiêu chí Sẵn sàng Phát hành (Pre-flight Release Criteria)

- [x] **Mã nguồn đồng bộ & Chu kỳ 4 Sprint:** Toàn bộ 389 commits trên các nhánh (`dev/kien`, `quocbinh`, `dev/ducanh`) đã tích hợp hoàn hảo vào nhánh `main` sau 4 Sprint làm việc (26/10/2026 – 20/11/2026).
- [x] **Kiểm thử Đơn vị & Tích hợp (Backend):** Toàn bộ test suites của 7 microservices (`./gradlew test`) đạt tỉ lệ thành công 100% (> 120 test cases).
- [x] **Kiểm thử Giao diện & Build (Frontend):** Frontend build hoàn tất (`npm run build`), không có cảnh báo cú pháp, hỗ trợ đầy đủ 4 phân hệ (Buyer, Seller, Admin, Shipper).
- [x] **Cơ sở dữ liệu phân tán:** Script `init.sql` khởi tạo đầy đủ các bảng dữ liệu cho 7 microservices và dữ liệu mẫu khởi tạo chuẩn hóa mã UTF-8.
- [x] **Gia cố An toàn Thông tin (18 QA Checklist):** Rà soát an toàn secrets, không rò rỉ private keys; cấu hình Nginx Security Headers (CSP, HSTS, X-Frame-Options); kiểm tra tính hợp lệ chữ ký Base64URL JWT; chặn SQLi fast-reject và kiểm soát IDOR.
- [x] **Thanh toán kép & Sổ cái kế toán:** Kiểm thử thành công cổng thanh toán VNPay Sandbox và cổng chuyển khoản tự động PayOS VietQR chuẩn EMVCo; Sổ cái kép (Debit = Credit) và Ký quỹ Escrow vận hành chính xác.
- [x] **Bản đồ số & Geocoding:** Leaflet OpenStreetMap và Nominatim GPS Geocoding tích hợp an toàn qua Nginx Tile Proxy.
- [x] **Xác thực Đa kênh:** Đăng nhập xã hội Google OAuth 2.0 và luồng xác thực mã OTP qua Email với Redis TTL 5 phút hoạt động ổn định.
- [x] **Tin nhắn thời gian thực:** Hệ thống Full-page Messenger giữa Buyer và Seller nhúng thẻ sản phẩm thông minh phản hồi tức thì.
- [x] **Quản lý Flash Sale AI:** Cổng Flash Sale đa chế độ (Manual & AI Auto-Approve) và đồng hồ đếm ngược đồng bộ hoàn tất.
- [x] **CI/CD Pipeline & Hạ tầng VPS:** GitHub Actions workflow `deploy.yml` hoàn tất; Docker Compose tối ưu JVM heap/metaspace chạy mượt mà trên VPS.
- [x] **Giám sát & Cảnh báo:** Prometheus TSDB, Grafana Dashboards và Zipkin Tracing đã cấu hình sẵn sàng thu thập số liệu.
- [x] **Hồ sơ Bàn giao & Tài liệu:** Đã cập nhật `README.md`, `PROJECT_PLAN_AND_WORK_BREAKDOWN_4_SPRINTS.md`, `QuickShop_Trello_Import_Updated.csv`, `HANDOVER_REPORT.md`, `USER_GUIDE.md` và các báo cáo QA.

---

## 2. Kế hoạch Dự phòng & Phục hồi (Rollback Strategy)

1. **Lỗi phiên bản ứng dụng:**
   - Trường hợp image mới gặp lỗi sau khi deploy lên VPS: Khôi phục container về tag ổn định trước đó (`IMAGE_TAG=previous`).
2. **Lỗi dữ liệu (Database corruption):**
   - Khôi phục cơ sở dữ liệu từ bản snapshot định kỳ của MySQL volume (`quickshop_mysql_prod_data`).
3. **Cơ chế Fallback Frontend:**
   - Frontend đã trang bị cơ chế tự động chuyển sang dữ liệu mẫu offline (Offline Mock Fallback) nếu backend tạm thời bảo trì hoặc mất kết nối mạng.
4. **Hết hạn ngạch Trí tuệ nhân tạo (Gemini API Quota):**
   - Notification AI Service tích hợp Circuit Breaker, tự động fallback sang gợi ý sản phẩm bán chạy nhất hệ thống khi quota cạn.
