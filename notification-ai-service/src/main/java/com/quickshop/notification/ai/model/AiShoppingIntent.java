package com.quickshop.notification.ai.model;

/**
 * Phân loại ý định người dùng trong trợ lý mua sắm QuickShop AI
 */
public enum AiShoppingIntent {
    PRODUCT_RECOMMENDATION, // Tư vấn, tìm kiếm gợi ý sản phẩm
    ORDER_INQUIRY,          // Tra cứu tình trạng đơn hàng, vận chuyển
    PROMOTION_DISCOUNT,     // Hỏi mã giảm giá, voucher sàn / shop
    SHIPPING_POLICY,        // Chính sách giao hàng, freeship, đổi trả
    CUSTOMER_SUPPORT,       // Hỗ trợ khiếu nại, liên hệ gian hàng / admin
    GENERAL_GREETING,       // Chào hỏi, giới thiệu tính năng
    OFF_TOPIC,              // Câu hỏi nằm ngoài phạm vi TMĐT QuickShop (từ chối lịch sự)
    SECURITY_VIOLATION      // Tấn công prompt injection hoặc can thiệp hệ thống
}
