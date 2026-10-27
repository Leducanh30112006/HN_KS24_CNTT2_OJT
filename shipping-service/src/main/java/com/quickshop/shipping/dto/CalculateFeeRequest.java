package com.quickshop.shipping.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateFeeRequest {

    private String fromProvince;
    private String toProvince;
    private String originAddress;
    private String destinationAddress;

    @Min(value = 1, message = "Trọng lượng phải lớn hơn 0")
    @Builder.Default
    private Integer weightGrams = 500;

    @Min(value = 1, message = "Chiều dài phải lớn hơn 0")
    private Integer lengthCm;

    @Min(value = 1, message = "Chiều rộng phải lớn hơn 0")
    private Integer widthCm;

    @Min(value = 1, message = "Chiều cao phải lớn hơn 0")
    private Integer heightCm;

    private BigDecimal insuranceValue;
    private String carrierName;
}
