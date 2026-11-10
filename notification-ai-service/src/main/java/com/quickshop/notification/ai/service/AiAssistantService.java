package com.quickshop.notification.ai.service;

import com.quickshop.notification.ai.dto.AiChatRequest;
import com.quickshop.notification.ai.dto.AiChatResponse;
import com.quickshop.notification.ai.dto.PromptVerifyDto;

import java.util.Map;

/**
 * Service quản lý toàn diện Trợ lý mua sắm QuickShop AI
 */
public interface AiAssistantService {

    /**
     * Xử lý hội thoại chat thông minh có kiểm soát an toàn Prompt Injection
     *
     * @param request Yêu cầu chat của người dùng
     * @return Phản hồi tư vấn mua sắm từ AI Assistant
     */
    AiChatResponse chat(AiChatRequest request);

    /**
     * Kiểm thử và thẩm định độ an toàn của prompt (dành cho QA / Security audit)
     *
     * @param request Yêu cầu kiểm thử
     * @return Chi tiết đánh giá mức độ rủi ro và các mẫu tấn công phát hiện
     */
    PromptVerifyDto.Response verifyPrompt(PromptVerifyDto.Request request);

    /**
     * Lấy siêu dữ liệu và chính sách bảo vệ an toàn AI (không làm lộ System Prompt bí mật)
     */
    Map<String, Object> getSafetyPolicy();
}

