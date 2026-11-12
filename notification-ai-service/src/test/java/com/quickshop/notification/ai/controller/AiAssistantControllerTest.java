package com.quickshop.notification.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.notification.ai.catalog.AiCatalogItemDto;
import com.quickshop.notification.ai.catalog.AiCatalogSearchRequest;
import com.quickshop.notification.ai.catalog.AiCatalogSearchResult;
import com.quickshop.notification.ai.catalog.AiCatalogSearchService;
import com.quickshop.notification.ai.catalog.CircuitBreakerState;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.notification.ai.dto.AiChatRequest;
import com.quickshop.notification.ai.dto.AiChatResponse;
import com.quickshop.notification.ai.dto.AiSeoGenerateRequest;
import com.quickshop.notification.ai.dto.AiSeoGenerateResponse;
import com.quickshop.notification.ai.dto.PromptVerifyDto;
import com.quickshop.notification.ai.model.AiShoppingIntent;
import com.quickshop.notification.ai.model.PromptRiskLevel;
import com.quickshop.notification.ai.service.AiAssistantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiAssistantController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AiAssistantController - Endpoints Chatbot AI & Kiểm thử an toàn Prompt")
class AiAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiAssistantService aiAssistantService;

    @MockBean
    private AiCatalogSearchService aiCatalogSearchService;

    @Test
    @DisplayName("POST /api/ai/chat - Trò chuyện tư vấn mua sắm thành công")
    void chat_Success() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Tư vấn điện thoại iPhone")
                .userId(1L)
                .build();

        AiChatResponse response = AiChatResponse.builder()
                .reply("Gợi ý QuickPhone Pro Max 256GB")
                .sanitizedPrompt("Tư vấn điện thoại iPhone")
                .riskLevel(PromptRiskLevel.SAFE)
                .intent(AiShoppingIntent.PRODUCT_RECOMMENDATION)
                .suggestedQuestions(List.of("Xem đánh giá", "Mã giảm giá"))
                .fallbackUsed(true)
                .latencyMs(12)
                .build();

        when(aiAssistantService.chat(any(AiChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").value("Gợi ý QuickPhone Pro Max 256GB"))
                .andExpect(jsonPath("$.data.riskLevel").value("SAFE"))
                .andExpect(jsonPath("$.data.intent").value("PRODUCT_RECOMMENDATION"));
    }

    @Test
    @DisplayName("POST /api/ai/verify-prompt - Thẩm định prompt an toàn thành công")
    void verifyPrompt_Success() throws Exception {
        PromptVerifyDto.Request req = PromptVerifyDto.Request.builder()
                .prompt("Tìm voucher giảm giá")
                .testCategory("PROMOTION")
                .build();

        PromptVerifyDto.Response res = PromptVerifyDto.Response.builder()
                .originalPrompt("Tìm voucher giảm giá")
                .sanitizedPrompt("Tìm voucher giảm giá")
                .riskLevel(PromptRiskLevel.SAFE)
                .allowed(true)
                .explanation("Prompt an toàn")
                .inspectedAt(System.currentTimeMillis())
                .build();

        when(aiAssistantService.verifyPrompt(any(PromptVerifyDto.Request.class))).thenReturn(res);

        mockMvc.perform(post("/api/ai/verify-prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.riskLevel").value("SAFE"));
    }

    @Test
    @DisplayName("GET /api/ai/policy - Lấy thông tin chính sách an toàn AI")
    void getPolicy_Success() throws Exception {
        when(aiAssistantService.getSafetyPolicy()).thenReturn(Map.of("assistantName", "QuickShop AI Shopping Assistant"));

        mockMvc.perform(get("/api/ai/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assistantName").value("QuickShop AI Shopping Assistant"));
    }

    @Test
    @DisplayName("GET /api/ai/catalog/search - Tìm kiếm sản phẩm catalog RAG thành công")
    void searchCatalog_Success() throws Exception {
        AiCatalogItemDto item = AiCatalogItemDto.builder()
                .id(1L)
                .name("Laptop QuickBook Ultra 14 inch")
                .categoryName("Laptop & Máy tính")
                .minPrice(BigDecimal.valueOf(45990000))
                .maxPrice(BigDecimal.valueOf(57990000))
                .totalStock(30)
                .averageRating(5.0)
                .status("ACTIVE")
                .build();

        AiCatalogSearchResult result = AiCatalogSearchResult.builder()
                .items(List.of(item))
                .snippets(List.of(item.toSnippet()))
                .totalFound(1)
                .source("CATALOG_SERVICE_LIVE")
                .fallbackUsed(false)
                .circuitBreakerState(CircuitBreakerState.CLOSED)
                .executionTimeMs(25)
                .build();

        when(aiCatalogSearchService.searchProducts(any(AiCatalogSearchRequest.class))).thenReturn(result);

        mockMvc.perform(get("/api/ai/catalog/search")
                        .param("keyword", "laptop")
                        .header("X-User-Role", "ROLE_BUYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalFound").value(1))
                .andExpect(jsonPath("$.data.circuitBreakerState").value("CLOSED"))
                .andExpect(jsonPath("$.data.items[0].name").value("Laptop QuickBook Ultra 14 inch"));
    }

    @Test
    @DisplayName("GET /api/ai/circuit-breaker - Tra cứu trạng thái Circuit Breaker")
    void getCircuitBreakerStatus_Success() throws Exception {
        Map<String, Object> statusMap = Map.of(
                "state", "CLOSED",
                "failureCount", 0,
                "failureThreshold", 3,
                "resetTimeoutMs", 30000L
        );

        when(aiCatalogSearchService.getCircuitBreakerStatus()).thenReturn(statusMap);

        mockMvc.perform(get("/api/ai/circuit-breaker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.state").value("CLOSED"))
                .andExpect(jsonPath("$.data.failureCount").value(0));
    }

    @Test
    @DisplayName("POST /api/ai/circuit-breaker/reset - Reset Circuit Breaker về CLOSED")
    void resetCircuitBreaker_Success() throws Exception {
        doNothing().when(aiCatalogSearchService).resetCircuitBreaker();
        when(aiCatalogSearchService.getCircuitBreakerStatus()).thenReturn(Map.of(
                "state", "CLOSED",
                "failureCount", 0
        ));

        mockMvc.perform(post("/api/ai/circuit-breaker/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.state").value("CLOSED"))
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    @DisplayName("POST /api/ai/generate-seo - Sinh mô tả sản phẩm chuẩn SEO thành công")
    void generateSeo_Success() throws Exception {
        AiSeoGenerateRequest request = AiSeoGenerateRequest.builder()
                .productName("Laptop QuickBook Ultra 14 Pro")
                .category("Laptop & Máy tính")
                .specifications(Map.of("RAM", "18GB", "SSD", "512GB"))
                .keywords("laptop mỏng nhẹ, chính hãng")
                .build();

        AiSeoGenerateResponse response = AiSeoGenerateResponse.builder()
                .optimizedTitle("[Chính Hãng] Laptop QuickBook Ultra 14 Pro")
                .fullDescription("### GIỚI THIỆU SẢN PHẨM\n- RAM 18GB, SSD 512GB hiệu năng cao.")
                .metaTitle("Laptop QuickBook Ultra 14 Pro | QuickShop")
                .metaDescription("Laptop QuickBook Ultra 14 Pro chính hãng tại QuickShop.")
                .slug("chinh-hang-laptop-quickbook-ultra-14-pro")
                .seoKeywords(List.of("laptop mỏng nhẹ", "chính hãng"))
                .outputFormat("MARKDOWN")
                .sanitized(false)
                .promptTruncated(false)
                .promptLengthUsed(140)
                .maxPromptLengthLimit(800)
                .outputValid(true)
                .fallbackUsed(true)
                .modelUsed("QuickShopSeoFallbackEngine")
                .build();

        when(aiAssistantService.generateSeoDescription(any(AiSeoGenerateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ai/generate-seo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.optimizedTitle").value("[Chính Hãng] Laptop QuickBook Ultra 14 Pro"))
                .andExpect(jsonPath("$.data.outputValid").value(true))
                .andExpect(jsonPath("$.data.slug").value("chinh-hang-laptop-quickbook-ultra-14-pro"));
    }

    @Test
    @DisplayName("POST /api/ai/generate-seo - Trả về 400 khi thiếu tên sản phẩm")
    void generateSeo_ValidationError() throws Exception {
        AiSeoGenerateRequest request = AiSeoGenerateRequest.builder()
                .productName("")
                .category("Laptop")
                .build();

        mockMvc.perform(post("/api/ai/generate-seo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.productName").exists());
    }

    @Test
    @DisplayName("POST /api/ai/generate-seo - Trả về 400 khi phát hiện prompt injection")
    void generateSeo_BlockedInjection() throws Exception {
        AiSeoGenerateRequest request = AiSeoGenerateRequest.builder()
                .productName("Điện thoại <script>alert(1)</script>")
                .build();

        when(aiAssistantService.generateSeoDescription(any(AiSeoGenerateRequest.class)))
                .thenThrow(new BadRequestException("Vi phạm kiểm soát an toàn prompt"));

        mockMvc.perform(post("/api/ai/generate-seo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Vi phạm kiểm soát an toàn prompt"));
    }
}

