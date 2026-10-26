package com.quickshop.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSeoRequest {
    private String productName;
    private String category;
    private String keywords;
    private String targetAudience;
}
