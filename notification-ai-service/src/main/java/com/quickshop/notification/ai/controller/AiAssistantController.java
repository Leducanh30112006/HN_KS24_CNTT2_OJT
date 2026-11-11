package com.quickshop.notification.ai.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.notification.ai.catalog.AiCatalogSearchRequest;
import com.quickshop.notification.ai.catalog.AiCatalogSearchResult;
import com.quickshop.notification.ai.catalog.AiCatalogSearchService;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý các chức năng của AI Assistant:
 * - Trò chuyện tư vấn mua sắm thông minh
 * - Tích hợp gọi Function tìm kiếm Catalog sản phẩm (RAG)
 * - Quản lý Circuit Breaker & Fallback cho AI Service
 * - Kiểm soát và ngăn chặn Prompt Injection
 * - Cung cấp endpoint thẩm định an toàn Prompt cho QA & Security Audit
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Shopping Assistant", description = "Trợ lý AI mua sắm thông minh QuickShop tích hợp phòng vệ Prompt Injection & Catalog RAG")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final AiCatalogSearchService aiCatalogSearchService;

    @PostMapping("/chat")
    @Operation(summary = "Trò chuyện mua sắm cùng QuickShop AI Assistant (chống Prompt Injection & Catalog Context)")
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

    @GetMapping("/catalog/search")
    @Operation(summary = "Tìm kiếm catalog tích hợp Circuit Breaker, kiểm tra quyền truy cập và Fallback cho AI Assistant")
    public ResponseEntity<ApiResponse<AiCatalogSearchResult>> searchCatalog(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "X-User-Role", defaultValue = "ROLE_BUYER") String userRole) {
        log.info("Catalog AI search request: keyword='{}', categoryId={}, limit={}, role={}", keyword, categoryId, limit, userRole);
        AiCatalogSearchRequest searchRequest = AiCatalogSearchRequest.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .limit(limit)
                .userRole(userRole)
                .build();
        AiCatalogSearchResult result = aiCatalogSearchService.searchProducts(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/circuit-breaker")
    @Operation(summary = "Tra cứu trạng thái của AI Catalog Circuit Breaker (CLOSED, OPEN, HALF_OPEN)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCircuitBreakerStatus() {
        return ResponseEntity.ok(ApiResponse.success(aiCatalogSearchService.getCircuitBreakerStatus()));
    }

    @PostMapping("/circuit-breaker/reset")
    @Operation(summary = "Reset AI Catalog Circuit Breaker về trạng thái CLOSED")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetCircuitBreaker() {
        aiCatalogSearchService.resetCircuitBreaker();
        Map<String, Object> resp = new HashMap<>(aiCatalogSearchService.getCircuitBreakerStatus());
        resp.put("message", "Circuit Breaker đã được reset về trạng thái CLOSED thành công.");
        return ResponseEntity.ok(ApiResponse.success("Reset Circuit Breaker thành công", resp));
    }
}

