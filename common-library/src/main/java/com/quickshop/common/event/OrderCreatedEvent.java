package com.quickshop.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private String orderId;
    private Long masterOrderId;
    private Long subOrderId;
    private Long shopId;
    private Long customerId;
    private String customerEmail;
    private List<OrderItemEventDto> items;
    private BigDecimal totalAmount;
    private String paymentMethod;

    @Builder.Default
    private Integer ttlSeconds = 900; // 15 phút TTL giữ kho

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
