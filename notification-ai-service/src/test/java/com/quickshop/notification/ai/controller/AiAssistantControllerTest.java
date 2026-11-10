package com.quickshop.notification.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.notification.ai.dto.AiChatRequest;
import com.quickshop.notification.ai.dto.AiChatResponse;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
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
}

