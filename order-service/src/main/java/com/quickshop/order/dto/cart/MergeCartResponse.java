package com.quickshop.order.dto.cart;

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
public class MergeCartResponse implements Serializable {

    private CartResponse cart;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private Integer mergedItemsCount;
    private boolean duplicateRequest;
}
