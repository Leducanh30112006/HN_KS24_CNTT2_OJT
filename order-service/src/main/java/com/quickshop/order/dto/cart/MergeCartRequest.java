package com.quickshop.order.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartRequest implements Serializable {

    private String requestId;

    @NotEmpty(message = "Danh sách mặt hàng guest cart không được để trống")
    @Valid
    @Builder.Default
    private List<GuestCartItemDto> items = new ArrayList<>();
}
