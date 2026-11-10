package com.quickshop.notification.ai.guardrail;

import com.quickshop.notification.ai.model.PromptSafetyResult;

/**
 * Dịch vụ kiểm soát an toàn và phòng chống Prompt Injection cho QuickShop AI Assistant
 */
public interface PromptGuardService {

    /**
     * Thẩm định toàn diện mức độ an toàn của prompt người dùng
     *
     * @param rawPrompt Tin nhắn thô từ người dùng
     * @return Kết quả kiểm tra an toàn kèm phân loại rủi ro (SAFE, SUSPICIOUS, BLOCKED)
     */
    PromptSafetyResult inspectPrompt(String rawPrompt);

    /**
     * Làm sạch và chuẩn hóa prompt loại bỏ các đoạn injection nguy hiểm
     *
     * @param rawPrompt Tin nhắn thô
     * @return Chuỗi đã được lọc và chuẩn hóa an toàn
     */
    String sanitize(String rawPrompt);

    /**
     * Kiểm tra nhanh xem prompt có nằm trong phạm vi thương mại điện tử hợp lệ hay không
     *
     * @param prompt Tin nhắn người dùng
     * @return true nếu thuộc phạm vi mua sắm / đơn hàng / hỗ trợ khách hàng, false nếu off-topic
     */
    boolean isShoppingScoped(String prompt);
}
