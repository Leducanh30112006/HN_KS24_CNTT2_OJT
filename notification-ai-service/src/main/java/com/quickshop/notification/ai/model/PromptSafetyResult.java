package com.quickshop.notification.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả thẩm định an toàn Prompt từ PromptGuardService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptSafetyResult {

    private boolean safe;
    private PromptRiskLevel riskLevel;
    private String originalPrompt;
    private String sanitizedPrompt;

    @Builder.Default
    private List<String> detectedThreats = new ArrayList<>();

    private String rejectionReason;

    public static PromptSafetyResult safe(String prompt, String sanitized) {
        return PromptSafetyResult.builder()
                .safe(true)
                .riskLevel(PromptRiskLevel.SAFE)
                .originalPrompt(prompt)
                .sanitizedPrompt(sanitized)
                .detectedThreats(new ArrayList<>())
                .build();
    }

    public static PromptSafetyResult suspicious(String prompt, String sanitized, List<String> threats) {
        return PromptSafetyResult.builder()
                .safe(true)
                .riskLevel(PromptRiskLevel.SUSPICIOUS)
                .originalPrompt(prompt)
                .sanitizedPrompt(sanitized)
                .detectedThreats(threats)
                .build();
    }

    public static PromptSafetyResult blocked(String prompt, String reason, List<String> threats) {
        return PromptSafetyResult.builder()
                .safe(false)
                .riskLevel(PromptRiskLevel.BLOCKED)
                .originalPrompt(prompt)
                .sanitizedPrompt("")
                .rejectionReason(reason)
                .detectedThreats(threats)
                .build();
    }
}

