package com.quickshop.shipping.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShipmentRequest {

    @NotNull(message = "subOrderId không được để trống")
    private Long subOrderId;

    @NotNull(message = "shopId không được để trống")
    private Long shopId;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String shippingAddress;

    private String originAddress;

    @Min(value = 1, message = "Trọng lượng phải lớn hơn 0")
    @Builder.Default
    private Integer weightGrams = 500;

    @Builder.Default
    private BigDecimal codAmount = BigDecimal.ZERO;

    private String carrierName; // GHN, GHTK, MOCK_CARRIER
}
