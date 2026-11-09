package com.quickshop.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class OrderDlqMessage implements Serializable {
    private String originalExchange;
    private String originalRoutingKey;
    private String payload;
    private String errorMessage;
    private String exceptionClass;
    private Integer retryCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime failedAt = LocalDateTime.now();
}
