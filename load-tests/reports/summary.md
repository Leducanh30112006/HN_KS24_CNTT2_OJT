
# Báo Cáo Kết Quả Kiểm Thử Tải QuickShop (k6 Load Test Report)
- **Thời gian chạy**: 2026-10-26T20:58:36.319Z
- **Mục tiêu tải**: 1000 RPS (Đọc: 800 RPS - 80%, Ghi: 200 RPS - 20%)
- **Mục tiêu kiểm thử**: Cart, Checkout, Dual Voucher, Distributed Locks, Idempotency

## 1. Số Liệu Tổng Hợp Thực Tế
| Chỉ số (Metric) | Kết quả đo thực tế | Ngưỡng cam kết (SLA) | Trạng thái |
| :--- | :--- | :--- | :--- |
| **Tổng số Request** | **46674 reqs** | N/A | Hoàn thành |
| **RPS Thực tế Đạt được** | **666.81 req/s** | >= 900 req/s | WARNING / BOTTLENECK |
| **Tỷ lệ Lỗi (Error Rate)** | **0.00%** | < 2.00% | PASS |
| **Độ trễ trung vị p50** | **0.00 ms** | < 150 ms | PASS |
| **Độ trễ phân vị p95** | **1.70 ms** | < 500 ms | PASS |
| **Độ trễ phân vị p99** | **N/A ms** | < 1000 ms | FAIL |
| **Độ trễ trung bình (Avg)** | **0.50 ms** | N/A | Tham khảo |
| **Độ trễ tối đa (Max)** | **42.18 ms** | N/A | Tham khảo |

## 2. Chi Tiết Từng Phân Hệ Nghiệp Vụ
| Phân hệ / API | Độ trễ p95 | Tỷ lệ thành công | Số lượng giao dịch |
| :--- | :--- | :--- | :--- |
| **Đọc dữ liệu (Read Workload)** | **0.52 ms** | 100.00% | 11289 tra cứu voucher |
| **Ghi dữ liệu (Write Workload)** | **11.12 ms** | 100.00% | 6593 cart actions |
| **Checkout Đơn hàng (Distributed Tx)** | **12.18 ms** | 100.00% | 2731 đơn Master Order tạo mới |
