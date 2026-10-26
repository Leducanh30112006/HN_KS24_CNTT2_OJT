package com.quickshop.notification.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.notification.dto.AiChatResponse;
import com.quickshop.notification.dto.AiSeoRequest;
import com.quickshop.notification.dto.AiSeoResponse;
import com.quickshop.notification.entity.Notification;
import com.quickshop.notification.repository.NotificationRepository;
import com.quickshop.notification.service.AiAssistantService;
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
    private final AiAssistantService aiAssistantService;

    @GetMapping("/api/notifications/my")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)));
    }

    @PostMapping("/api/ai/chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatWithAi(@RequestBody AiChatRequest request) {
        String userInput = request.getMessage() != null ? request.getMessage() : "";
        AiChatResponse chatResponse = aiAssistantService.processShoppingChat(userInput);

        Map<String, Object> response = new HashMap<>();
        response.put("reply", chatResponse.getReply());
        response.put("originalPrompt", chatResponse.getOriginalPrompt());
        response.put("sanitizedPrompt", chatResponse.getSanitizedPrompt());
        response.put("suggestions", chatResponse.getSuggestions());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/api/ai/generate-seo")
    public ResponseEntity<ApiResponse<AiSeoResponse>> generateSeo(@RequestBody AiSeoRequest request) {
        AiSeoResponse response = aiAssistantService.generateSeoContent(request);
        return ResponseEntity.ok(ApiResponse.success("Sinh nội dung SEO thành công", response));
    }

    @Data
    public static class AiChatRequest {
        private String message;
    }
}
