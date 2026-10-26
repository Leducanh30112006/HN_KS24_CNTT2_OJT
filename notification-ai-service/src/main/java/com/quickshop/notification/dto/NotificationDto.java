package com.quickshop.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto implements Serializable {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private boolean isRead;
    private LocalDateTime createdAt;
}
