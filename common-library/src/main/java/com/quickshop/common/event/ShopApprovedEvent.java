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
public class ShopApprovedEvent implements Serializable {
    private Long shopId;
    private Long ownerId;
    private String shopName;
    private String ownerEmail;
    private String ownerFullName;
    private String businessType;
    private String citizenId;
    private Long approvedBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime approvedAt = LocalDateTime.now();
}
