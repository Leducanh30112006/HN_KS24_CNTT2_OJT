package com.quickshop.notification.ai.dto;

import com.quickshop.notification.ai.model.AiShoppingIntent;
import com.quickshop.notification.ai.model.PromptRiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Phản hồi thông minh từ QuickShop AI Assistant")
public class AiChatResponse {

    @Schema(description = "Câu trả lời của trợ lý AI")
    private String reply;

    @Schema(description = "Prompt sau khi đã qua bộ lọc làm sạch an toàn")
    private String sanitizedPrompt;

    @Schema(description = "Mức độ rủi ro đánh giá của Prompt")
    private PromptRiskLevel riskLevel;

    @Schema(description = "Ý định mua sắm nhận diện được")
    private AiShoppingIntent intent;

    @Builder.Default
    @Schema(description = "Gợi ý các câu hỏi tiếp theo người dùng có thể quan tâm")
    private List<String> suggestedQuestions = new ArrayList<>();

    public List<String> getSuggestions() {
        return suggestedQuestions;
    }

    @Schema(description = "Có sử dụng cơ chế dữ liệu fallback an toàn hay không")
    private boolean fallbackUsed;

    @Schema(description = "Thời gian xử lý tính bằng mili-giây")
    private long latencyMs;

    @Schema(description = "Thời điểm phản hồi")
    private String timestamp;
}
