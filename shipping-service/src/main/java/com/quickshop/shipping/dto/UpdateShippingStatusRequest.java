package com.quickshop.shipping.dto;

import com.quickshop.shipping.entity.enums.ShippingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShippingStatusRequest {

    @NotNull(message = "newStatus không được để trống")
    private ShippingStatus newStatus;

    private String note;
}
