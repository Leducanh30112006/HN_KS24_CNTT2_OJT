# DANH SÁCH CÁC VẤN ĐỀ TỒN ĐỌNG & GIẢI PHÁP ĐỀ XUẤT (KNOWN ISSUES)

**Dự án:** Sàn Thương Mại Điện Tử Đa Người Bán QuickShop (Marketplace Edition)  
**Phiên bản:** Release 1.0 (Production Final - Kết thúc Sprint 4)  
**Ngày tổng hợp:** **20/11/2026**  
**Người tổng hợp:** Nguyễn Văn Kiên & Ban Kỹ thuật QuickShop  

---

## 1. Danh sách vấn đề tồn đọng & Tình trạng xử lý

| ID | Mức độ | Phân hệ | Mô tả vấn đề | Giải pháp tạm thời (Workaround) | Tình trạng & Kế hoạch xử lý |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ISSUE-01** | Low | Payment Service | VNPay Sandbox phản hồi phụ thuộc đối tác thử nghiệm | Cung cấp tùy chọn COD và cổng thanh toán tự động VietQR PayOS | **ĐÃ GIẢI QUYẾT TRONG SPRINT 4:** Tích hợp PayOS VietQR chuẩn EMVCo thanh toán tức thì |
| **ISSUE-02** | Low | Shipping Service | Webhook từ GHN/GHTK thực tế cần cấu hình URL công khai (Public Webhook URL) khi test local | Hệ thống đã cung cấp API Mock Simulator và Kênh bưu tá `/api/shipping/simulate-step/{trackingCode}` | Cấu hình Nginx SSL Reverse Proxy trên VPS staging/production sẵn sàng |
| **ISSUE-03** | Low | Notification AI | Khi hạn ngạch (Quota) Gemini API chạm ngưỡng miễn phí (Free Tier Rate Limit) | Service đã trang bị Circuit Breaker tự động fallback sang gợi ý sản phẩm bán chạy | Đăng ký gói Enterprise Gemini API có rate limit cao hơn khi mở rộng quy mô |
| **ISSUE-04** | Low | Order Service | RabbitMQ khởi động chậm hơn Order Service khi chạy Docker Compose lạnh | Docker compose đã cấu hình `restart: always` và healthcheck dependency | Hoạt động hoàn toàn ổn định qua cơ chế tự động kết nối lại (Auto-reconnect) |

---

## 2. Đánh giá rủi ro bàn giao
Không có vấn đề nào ở mức **Critical** hoặc **High** cản trở việc phát hành và nghiệm thu hệ thống giai đoạn 1.0. Các vấn đề tồn đọng đều ở mức Low, nằm trong khả năng kiểm soát và đã có giải pháp dự phòng hoàn chỉnh.
