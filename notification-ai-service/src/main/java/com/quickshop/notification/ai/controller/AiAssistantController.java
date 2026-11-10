package com.quickshop.notification.ai.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.notification.ai.dto.AiChatRequest;
import com.quickshop.notification.ai.dto.AiChatResponse;
import com.quickshop.notification.ai.dto.PromptVerifyDto;
import com.quickshop.notification.ai.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xử lý các chức năng của AI Assistant:
 * - Trò chuyện tư vấn mua sắm thông minh
 * - Kiểm soát và ngăn chặn Prompt Injection
 * - Cung cấp endpoint thẩm định an toàn Prompt cho QA & Security Audit
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Shopping Assistant", description = "Trợ lý AI mua sắm thông minh QuickShop tích hợp phòng vệ Prompt Injection")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    @Operation(summary = "Trò chuyện mua sắm cùng QuickShop AI Assistant (chống Prompt Injection)")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(@Valid @RequestBody AiChatRequest request) {
        log.info("Incoming AI chat request: user={}, messageLength={}", request.getUserId(),
                request.getMessage() != null ? request.getMessage().length() : 0);
        AiChatResponse response = aiAssistantService.chat(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/verify-prompt")
    @Operation(summary = "Kiểm thử và đánh giá độ an toàn của prompt (bộ test prompt cho QA/Security)")
    public ResponseEntity<ApiResponse<PromptVerifyDto.Response>> verifyPrompt(@Valid @RequestBody PromptVerifyDto.Request request) {
        log.info("Prompt security verification request: category={}", request.getTestCategory());
        PromptVerifyDto.Response response = aiAssistantService.verifyPrompt(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/policy")
    @Operation(summary = "Tra cứu thông tin chính sách an toàn và bảo vệ prompt của hệ thống AI")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPolicy() {
        return ResponseEntity.ok(ApiResponse.success(aiAssistantService.getSafetyPolicy()));
    }
}
