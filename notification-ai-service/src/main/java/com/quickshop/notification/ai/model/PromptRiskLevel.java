package com.quickshop.notification.ai.model;

/**
 * Mức độ rủi ro an toàn của Prompt gửi vào AI Assistant
 */
public enum PromptRiskLevel {
    /**
     * An toàn - Câu hỏi mua sắm bình thường trong phạm vi hệ thống
     */
    SAFE,

    /**
     * Đáng ngờ - Chứa từ khóa nhạy cảm hoặc định dạng đặc biệt, cần lọc kỹ trước khi xử lý
     */
    SUSPICIOUS,

    /**
     * Bị chặn - Tấn công prompt injection, jailbreak, trích xuất dữ liệu hoặc hành vi nguy hiểm
     */
    BLOCKED
}

