# CHECKLIST PHÁT HÀNH HỆ THỐNG (RELEASE CHECKLIST)

**Dự án:** QuickShop Multi-Vendor Marketplace  
**Phiên bản phát hành:** Release 1.0 (Final)  
**Thời gian:** 16/11/2026  
**Điều phối viên:** Lê Đức Anh  

---

## 1. Tiêu chí Sẵn sàng Phát hành (Pre-flight Release Criteria)

- [x] **Mã nguồn đồng bộ:** Tất cả các nhánh tính năng của các thành viên (`dev/kien`, `quocbinh`, `dev/ducanh`) đã hoàn thành, tích hợp và kiểm thử.
- [x] **Kiểm thử Đơn vị & Tích hợp:** Toàn bộ test suites backend (`./gradlew test`) đạt tỉ lệ thành công 100%.
- [x] **Kiểm thử Giao diện & Build:** Frontend build hoàn tất (`npm run build`), không có cảnh báo hay lỗi cú pháp.
- [x] **Cơ sở dữ liệu:** Script `init.sql` khởi tạo đầy đủ các bảng dữ liệu cho 6 service và dữ liệu mẫu khởi tạo.
- [x] **Bảo mật:** Rà soát an toàn secrets, không rò rỉ private keys hay mật khẩu trong commit history; CORS và Rate Limiting đã kích hoạt.
- [x] **CI/CD Pipeline:** GitHub Actions workflow `deploy.yml` đã kiểm tra build test và Docker packaging sẵn sàng.
- [x] **Giám sát & Cảnh báo:** Prometheus, Grafana và Zipkin đã tích hợp trong docker-compose.
- [x] **Tài liệu bàn giao:** Đã hoàn thiện README, .env.example, Hướng dẫn sử dụng và Báo cáo kiểm thử.

---

## 2. Kế hoạch Dự phòng & Phục hồi (Rollback Strategy)
1. **Lỗi phiên bản ứng dụng:**
   - Trường hợp image mới gặp lỗi sau khi deploy lên VPS: Khôi phục container về tag ổn định trước đó (`IMAGE_TAG=previous`).
2. **Lỗi dữ liệu (Database corruption):**
   - Khôi phục cơ sở dữ liệu từ bản snapshot định kỳ của MySQL volume (`quickshop_mysql_prod_data`).
3. **Cơ chế Fallback Frontend:**
   - Frontend đã trang bị cơ chế tự động chuyển sang dữ liệu mẫu offline (Offline Mock Fallback) nếu backend tạm thời bảo trì hoặc mất kết nối mạng.
