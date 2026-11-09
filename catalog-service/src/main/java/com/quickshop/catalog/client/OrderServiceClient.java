package com.quickshop.catalog.client;

/**
 * Client giao tiếp với Order Service để xác thực trạng thái đơn hàng (Order Contract)
 */
public interface OrderServiceClient {

    /**
     * Xác thực đơn hàng con (SubOrder) đã hoàn thành (COMPLETED) và đủ điều kiện đánh giá
     * @param subOrderId ID đơn hàng con
     * @param userId ID người mua
     * @param productId ID sản phẩm được đánh giá
     * @return true nếu đơn hàng COMPLETED và chứa sản phẩm, false nếu chưa đủ điều kiện
     */
    boolean isSubOrderCompleted(Long subOrderId, Long userId, Long productId);
}
