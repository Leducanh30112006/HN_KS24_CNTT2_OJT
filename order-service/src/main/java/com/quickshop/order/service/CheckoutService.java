package com.quickshop.order.service;

import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.order.MasterOrderResponse;

public interface CheckoutService {

    /**
     * Tiến hành Checkout đơn hàng đa shop từ giỏ hàng hoặc danh sách mặt hàng được chỉ định (SRS 2.2.0 - FR-06).
     *
     * @param userId               ID người mua (Buyer)
     * @param idempotencyKeyHeader Khóa chống trùng lặp từ HTTP Header X-Idempotency-Key (nếu có)
     * @param request              Dữ liệu checkout: địa chỉ giao hàng, phương thức thanh toán, voucher, items
     * @return MasterOrderResponse Thông tin Master Order và danh sách Sub-Orders theo từng Shop
     */
    MasterOrderResponse checkout(Long userId, String idempotencyKeyHeader, CheckoutRequest request);

    /**
     * Tra cứu chi tiết Master Order theo ID của người dùng.
     *
     * @param userId        ID người dùng
     * @param masterOrderId ID Master Order
     * @return MasterOrderResponse
     */
    MasterOrderResponse getOrderById(Long userId, Long masterOrderId);
}
