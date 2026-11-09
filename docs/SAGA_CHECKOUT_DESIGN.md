# KIẾN TRÚC SAGA CHECKOUT ORCHESTRATOR & EVENT CONTRACTS

> **Tài liệu đặc tả kiến trúc điều phối giao dịch phân tán (Saga Pattern)**  
> **Phân hệ:** Order, Catalog, Payment, Notification  
> **Người thiết kế:** Nguyễn Văn Kiên  
> **Ngày phê duyệt:** 07/11/2026 - 09/11/2026  
> **Phiên bản:** 1.0.0

---

## 1. Tổng quan Kiến trúc Saga Orchestration

Trong hệ thống E-Commerce QuickShop với kiến trúc Microservices và mô hình **Database-per-Service**, một giao dịch đặt mua hàng (Checkout Flow) trải dài qua nhiều dịch vụ độc lập:
- **Order Service:** Đóng vai trò **Saga Orchestrator** điều phối toàn bộ vòng đời đơn hàng, quản lý trạng thái Master/Sub-Orders và Voucher.
- **Catalog Service:** Quản lý kho hàng, giữ tồn kho (Reserve Stock có TTL 15 phút), giải phóng tồn kho (Release Stock khi hủy) và trừ kho vĩnh viễn (Commit Stock khi thanh toán thành công).
- **Payment Service:** Xử lý thanh toán trực tuyến qua cổng VNPay Sandbox hoặc đối soát đơn COD, quản lý Tài khoản Tạm giữ (Escrow) và Ví Seller.
- **Notification Service:** Lắng nghe các sự kiện đơn hàng qua RabbitMQ để gửi email OTP/xác nhận và thông báo đẩy.

Giao tiếp giữa các services được thực hiện bất đồng bộ (Asynchronous Messaging) thông qua **RabbitMQ Topic Exchange** kèm cơ chế **Dead-Letter Queue (DLQ)** để xử lý các sự cố nghiêm trọng sau tối đa 3 lần retry.

---

## 2. Mermaid Sequence Diagrams

### 2.1 Luồng Đặt hàng Thành công (Happy Path)

```mermaid
sequenceDiagram
    autonumber
    actor Buyer as Người mua (Buyer)
    participant OS as Order Service (Orchestrator)
    participant Redis as Redis Cache
    participant Rabbit as RabbitMQ (order.exchange)
    participant CS as Catalog Service
    participant PS as Payment Service
    participant NS as Notification Service

    Buyer->>OS: POST /api/orders/checkout (Items, Voucher, Address)
    OS->>OS: Tạo MasterOrder & SubOrders (Trạng thái DRAFT / PENDING_PAYMENT)
    OS->>Rabbit: Publish OrderCreatedEvent (order.created)
    Rabbit->>CS: Consumer nhận OrderCreatedEvent
    CS->>CS: Atomic Check & Reserve Stock trong DB
    CS->>Redis: Set Key `inventory:reserve:{orderId}` (TTL = 15 phút / 900s)
    CS->>Rabbit: Publish StockReservedEvent (stock.reserved - success=true)
    Rabbit->>OS: Orchestrator nhận StockReservedEvent
    OS->>OS: Chuyển MasterOrder sang PENDING_PAYMENT
    OS->>Rabbit: Publish PaymentRequestedEvent (payment.requested)
    
    alt Thanh toán VNPay
        Rabbit->>PS: Payment Service nhận PaymentRequestedEvent
        PS->>PS: Sinh VNPay Sandbox URL
        Buyer->>PS: Thanh toán thành công (VNPay IPN Callback)
        PS->>Rabbit: Publish PaymentCompletedEvent (payment.completed)
    else Thanh toán COD
        OS->>OS: Chuyển MasterOrder sang PROCESSING
        OS->>Rabbit: Publish PaymentCompletedEvent (payment.completed - COD confirmed)
    end

    Rabbit->>OS: Orchestrator nhận PaymentCompletedEvent
    OS->>OS: Cập nhật MasterOrder = PROCESSING, SubOrders = CONFIRMED
    OS->>Rabbit: Publish OrderConfirmedEvent (order.confirmed)
    Par Commit Stock & Gửi Thông Báo
        Rabbit->>CS: Catalog Service nhận OrderConfirmedEvent -> Commit Stock (xóa reserved)
        Rabbit->>NS: Notification Service nhận OrderConfirmedEvent -> Gửi Email xác nhận
    End
    OS-->>Buyer: Đặt hàng thành công 201 Created kèm Order Code
```

---

### 2.2 Luồng Thất bại & Giao dịch Bồi hoàn (Failure & Compensation Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Buyer as Người mua (Buyer)
    participant OS as Order Service (Orchestrator)
    participant Rabbit as RabbitMQ
    participant CS as Catalog Service
    participant Scheduler as OrderExpirationScheduler

    Note over OS,CS: Kịch bản 1: Không đủ tồn kho khi đặt hàng
    OS->>Rabbit: Publish OrderCreatedEvent (order.created)
    Rabbit->>CS: Consumer nhận OrderCreatedEvent
    CS->>CS: Kiểm tra kho -> Tồn kho không đủ / SKU ngừng bán
    CS->>Rabbit: Publish StockReservedEvent (stock.failed - success=false)
    Rabbit->>OS: Orchestrator nhận StockReservedEvent
    OS->>OS: Hủy đơn hàng MasterOrder & SubOrders -> CANCELLED
    OS->>OS: Hoàn trả Voucher sàn và Voucher Shop đã dùng

    Note over OS,CS: Kịch bản 2: Quá hạn thanh toán 15 phút (Timeout Compensation)
    Scheduler->>OS: Quét định kỳ mỗi 5 phút đơn DRAFT / PENDING_PAYMENT > 15 phút
    OS->>OS: Cập nhật MasterOrder -> CANCELLED (Lý do: Quá hạn thanh toán 15 phút)
    OS->>Rabbit: Publish OrderCompensationEvent (order.compensation / stock.release)
    Rabbit->>CS: Catalog Service nhận lệnh bồi hoàn
    CS->>CS: releaseStock() -> Hoàn lại stock khả dụng, giảm reservedStock
    CS->>CS: Xóa key Redis `inventory:reserve:{orderId}`
    OS->>OS: Hoàn trả số lượt sử dụng Voucher
```

---

### 2.3 Luồng Retry 3 Lần & Dead-Letter Queue (`order.dlq`)

```mermaid
sequenceDiagram
    autonumber
    participant Publisher as Service gửi sự kiện
    participant Rabbit as RabbitMQ Main Queue
    participant Consumer as Service nhận sự kiện
    participant DLX as Direct Exchange (order.dlx.exchange)
    participant DLQ as Dead-Letter Queue (order.dlq)
    participant Ops as Kỹ sư Vận hành / Admin

    Publisher->>Rabbit: Gửi tin nhắn sự kiện
    Rabbit->>Consumer: Giao tin nhắn lần 1 -> Xảy ra Exception / Network Timeout
    Note over Consumer: Thử lại lần 1 thất bại
    Consumer-->>Rabbit: Nack / Retry sau 1.000ms
    Rabbit->>Consumer: Giao tin nhắn lần 2 -> Tiếp tục Exception
    Note over Consumer: Thử lại lần 2 thất bại
    Consumer-->>Rabbit: Nack / Retry sau 2.000ms (Exponential Backoff)
    Rabbit->>Consumer: Giao tin nhắn lần 3 -> Tiếp tục Exception
    Note over Consumer: Đạt ngưỡng tối đa 3 lần retry!

    Consumer->>DLX: RepublishMessageRecoverer chuyển tin nhắn sang DLX
    DLX->>DLQ: Lưu tin nhắn vào hàng đợi `order.dlq`
    DLQ->>Ops: OrderDlqListener ghi Log ERROR có cảnh báo mức CRITICAL & REST API tra cứu
```

---

## 3. Ma trận Hợp đồng Sự kiện (Event Contracts Matrix)

| Tên Event Class | Exchange | Routing Key | Dịch vụ Phát | Dịch vụ Nhận | Mục đích nghiệp vụ |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `OrderCreatedEvent` | `order.exchange` | `order.created` | Order Service | Catalog Service | Kích hoạt giữ tồn kho (Reserve Stock) cho danh sách SKU |
| `StockReservedEvent` | `catalog.exchange` | `stock.reserved` | Catalog Service | Order Service | Báo kết quả giữ kho thành công (`success=true`) |
| `StockReservedEvent` | `catalog.exchange` | `stock.failed` | Catalog Service | Order Service | Báo giữ kho thất bại (`success=false`), kèm lý do chi tiết |
| `PaymentRequestedEvent`| `order.exchange` | `payment.requested` | Order Service | Payment Service | Yêu cầu chuẩn bị thanh toán VNPay hoặc đối soát COD |
| `PaymentCompletedEvent`| `order.exchange` | `payment.completed` | Payment Service | Order Service | Báo nhận tiền thành công từ cổng VNPay / xác nhận COD |
| `PaymentFailedEvent` | `order.exchange` | `payment.failed` | Payment Service | Order Service | Báo lỗi giao dịch thanh toán để kích hoạt bồi hoàn hủy đơn |
| `OrderConfirmedEvent` | `order.exchange` | `order.confirmed` | Order Service | Catalog, Notification | Xác nhận đơn hoàn tất; Catalog trừ kho vĩnh viễn, gửi email |
| `OrderCompensationEvent` / `StockReleaseEvent` | `order.exchange` | `stock.release` | Order Service | Catalog Service | Lệnh bồi hoàn giải phóng tồn kho (Release Stock) |
| `OrderDlqMessage` | `order.dlx.exchange` | `order.dlq.routing` | RabbitMQ Recoverer | Order Service (DLQ Listener) | Lưu trữ và giám sát các sự kiện xử lý thất bại sau 3 lần retry |

---

## 4. Ma trận Trạng thái Đơn hàng (State Machine Transitions)

| Trạng thái Hiện tại | Sự kiện Kích hoạt | Trạng thái Kế tiếp | Hành động Kèm theo |
| :--- | :--- | :--- | :--- |
| `(Khởi tạo)` | Buyer Checkout giỏ hàng | `DRAFT` | Tạo đơn hàng, snapshot SKU/giá, phát `order.created` |
| `DRAFT` | `stock.reserved` (COD) | `PROCESSING` | Giữ kho thành công, đơn COD chuyển thẳng sang xử lý |
| `DRAFT` | `stock.reserved` (Online VNPay) | `PENDING_PAYMENT` | Giữ kho thành công, tạo yêu cầu thanh toán (TTL 15 phút) |
| `DRAFT` | `stock.failed` | `CANCELLED` | Kho hết hàng, hủy đơn ngay lập tức, hoàn lại voucher |
| `PENDING_PAYMENT` | `payment.completed` | `PROCESSING` | Khách thanh toán VNPay thành công, chuyển xử lý đơn |
| `PENDING_PAYMENT` | `payment.failed` / Timeout 15m | `CANCELLED` | Quá hạn 15 phút, phát lệnh nhả kho (`stock.release`), hủy đơn |
| `PROCESSING` | Shop xác nhận đơn | `CONFIRMED` | Gửi lệnh trừ kho vĩnh viễn (commit reserved stock) |
| `CONFIRMED` | Shop đóng gói xong | `PACKING` | Chuẩn bị hàng giao cho đơn vị vận chuyển |
| `PACKING` | Bàn giao hãng vận chuyển | `SHIPPING` | Tạo vận đơn và mã tracking GHN/GHTK |
| `SHIPPING` | Giao thành công | `DELIVERED` | Buyer nhận hàng, kích hoạt đếm ngược 3 ngày Escrow |
| `DELIVERED` | Quá 3 ngày không khiếu nại | `COMPLETED` | Giải phóng tiền Escrow vào ví khả dụng của Seller |
