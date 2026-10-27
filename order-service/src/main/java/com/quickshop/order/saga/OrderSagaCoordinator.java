package com.quickshop.order.saga;

import com.quickshop.common.event.StockReservedEvent;
import com.quickshop.order.entity.MasterOrder;

/**
 * Saga Orchestrator cho chuỗi giao dịch phân tán (Saga Workflow) theo SRS QuickShop 2.2.0 - FR-06:
 * 1. Checkout tạo Master Order và Sub-Orders.
 * 2. Order Service gửi lệnh Reserve Stock sang Catalog Service qua RabbitMQ (kèm correlationId).
 * 3. Catalog Service giữ kho trong 15 phút và trả về kết quả StockReservedEvent.
 * 4. Nếu giữ kho thành công: Chuyển Master Order sang PENDING_PAYMENT, tiếp tục thanh toán.
 * 5. Nếu giữ kho thất bại: Kích hoạt Compensating Actions (hủy đơn, hoàn lại voucher, KHÔNG hoàn kho vì chưa giữ).
 * 6. Nếu quá 15 phút chưa thanh toán: Quét timeout, hủy đơn, hoàn voucher và gửi lệnh Release Stock sang Catalog.
 * 7. Chỉ bù trừ những bước đã thành công.
 * 8. Xử lý message trùng, sai thứ tự và lỗi tạm thời.
 */
public interface OrderSagaCoordinator {

    /**
     * Kích hoạt Saga Workflow cho Master Order vừa được tạo: Gửi lệnh Reserve Stock sang Catalog Service.
     *
     * @param masterOrder Master Order đã lưu trong CSDL
     */
    void initiateCheckoutSaga(MasterOrder masterOrder);

    /**
     * Xử lý khi Catalog Service giữ kho thành công: Chuyển đơn sang PENDING_PAYMENT.
     *
     * @param event         StockReservedEvent(success = true)
     * @param correlationId Mã Correlation ID truy vết phân tán
     */
    void handleStockReservedSuccess(StockReservedEvent event, String correlationId);

    /**
     * Xử lý khi Catalog Service giữ kho thất bại: Bù trừ hủy đơn và hoàn lại voucher.
     *
     * @param event         StockReservedEvent(success = false)
     * @param correlationId Mã Correlation ID truy vết phân tán
     */
    void handleStockReservedFailed(StockReservedEvent event, String correlationId);

    /**
     * Quét và hủy các đơn hàng quá hạn 15 phút chưa thanh toán (Compensating: Hủy đơn + Hoàn voucher + Release kho).
     *
     * @return Số lượng đơn hàng quá hạn đã được xử lý
     */
    int cancelExpiredOrders();
}
