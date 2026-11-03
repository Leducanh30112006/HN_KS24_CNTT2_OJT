package com.quickshop.order.dto.cart;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO cung cấp thông tin quản trị và trạng thái vòng đời của Redis Cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartMetadataDto implements Serializable {

    private String cartKey;
    private CartType cartType;
    private String ownerId;
    private Integer totalItems;
    private Long ttlSeconds;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    private boolean exists;
}
