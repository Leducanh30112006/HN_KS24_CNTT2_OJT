# DANH SÁCH CÁC VẤN ĐỀ TỒN ĐỌNG & GIẢI PHÁP ĐỀ XUẤT (KNOWN ISSUES)

**Dự án:** Sàn Thương Mại Điện Tử QuickShop (Multi-vendor)  
**Phiên bản:** Release Candidate 1.0  
**Ngày tổng hợp:** 16/11/2026  
**Người tổng hợp:** Nguyễn Văn Kiên & Ban Kỹ thuật QuickShop  

---

## 1. Danh sách vấn đề tồn đọng (Known Issues)

| ID | Mức độ | Phân hệ | Mô tả vấn đề | Giải pháp tạm thời (Workaround) | Kế hoạch xử lý lâu dài (Roadmap) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ISSUE-01** | Low | Payment Service | VNPay hiện tại chạy trên môi trường Sandbox công khai của VNPay, đôi khi phản hồi chậm vào giờ cao điểm của đối tác | Người dùng có thể nhấn thử lại hoặc chọn hình thức COD | Tích hợp thêm kênh thanh toán dự phòng (MoMo, ZaloPay, VietQR) |
| **ISSUE-02** | Low | Shipping Service | Webhook từ GHN/GHTK thực tế cần cấu hình URL công khai (Public Webhook URL / Ngrok) khi chạy cục bộ | Hệ thống đã cung cấp API Mock Simulator `/api/shipping/simulate-step/{trackingCode}` để test local | Đăng ký IP Whitelist và Domain SSL cố định trên server staging/production |
| **ISSUE-03** | Low | Notification AI | Khi hạn ngạch (Quota) Gemini API chạm ngưỡng miễn phí (Free Tier Rate Limit), khuyến nghị sản phẩm có thể fallback sang gợi ý theo danh mục bán chạy | Service đã có cơ chế fallback trả về danh sách sản phẩm phổ biến | Đăng ký gói Enterprise Gemini API có rate limit cao hơn |
| **ISSUE-04** | Low | Order Service | Trong trường hợp RabbitMQ khởi động chậm hơn Order Service khi chạy Docker Compose lạnh, log có thể cảnh báo kết nối lại ban đầu | Docker compose đã cấu hình `restart: always` và healthcheck | Thêm script `wait-for-it` hoặc service dependency healthcheck tối ưu |

---

## 2. Đánh giá rủi ro bàn giao
Không có vấn đề nào ở mức **Critical** hoặc **High** cản trở việc phát hành và nghiệm thu hệ thống giai đoạn 1.0. Các vấn đề tồn đọng đều nằm trong khả năng kiểm soát và đã có giải pháp dự phòng hoàn chỉnh.
