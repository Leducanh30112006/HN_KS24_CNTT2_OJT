package com.quickshop.notification.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.notification.entity.Notification;
import com.quickshop.notification.repository.NotificationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class NotificationAiController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/api/notifications/my")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)));
    }

    @PostMapping("/api/ai/chat")
    public ResponseEntity<ApiResponse<Map<String, String>>> chatWithAi(@RequestBody AiChatRequest request) {
        String userInput = request.getMessage() != null ? request.getMessage() : "";

        // FR-10: Kiểm tra Prompt Sanitization chống Prompt Injection
        String sanitizedPrompt = sanitizePrompt(userInput);
        log.info("AI Shopping Assistant processing sanitized query: {}", sanitizedPrompt);

        // Fallback / Circuit Breaker shopping response
        String reply;
        String lower = sanitizedPrompt.toLowerCase();
        if (lower.contains("điện thoại") || lower.contains("phone")) {
            reply = "Chào bạn! Mình gợi ý bạn tham khảo dòng **QuickPhone Pro Max 256GB** tại gian hàng TechWorld Official Store, hiện đang có sẵn màu Titan Tự Nhiên với giá ưu đãi cực tốt!";
        } else if (lower.contains("laptop") || lower.contains("máy tính")) {
            reply = "Bạn có thể tham khảo **QuickBook Ultra 14 inch M3 Pro** cấu hình 16GB/512GB siêu mượt cho lập trình viên và đồ họa nhé!";
        } else {
            reply = "Chào bạn! Tôi là Trợ lý Mua sắm QuickShop AI. Bạn cần tư vấn sản phẩm, kiểm tra khuyến mãi hay theo dõi đơn hàng hôm nay?";
        }

        Map<String, String> response = new HashMap<>();
        response.put("reply", reply);
        response.put("originalPrompt", userInput);
        response.put("sanitizedPrompt", sanitizedPrompt);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String sanitizePrompt(String input) {
        if (input == null) return "";
        // Loại bỏ các mẫu injection thường gặp
        String cleaned = input.replaceAll("(?i)(ignore previous instructions|drop database|system prompt|reveal secrets)", "[FILTERED]");
        return cleaned.trim();
    }

    @Data
    public static class AiChatRequest {
        private String message;
    }
}
