package com.quickshop.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingMilestoneDto {
    private String status;
    private String title;
    private String description;
    private String location;
    private LocalDateTime timestamp;
    private boolean completed;
    private boolean current;
}

