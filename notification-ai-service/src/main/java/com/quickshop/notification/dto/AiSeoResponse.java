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
public class AiSeoResponse {
    private String optimizedTitle;
    private List<String> highlights;
    private String metaDescription;
    private List<String> tags;
    private String fullDescription;
}
