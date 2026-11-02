package com.quickshop.catalog.dto;

import com.quickshop.catalog.entity.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    private Long categoryId;
    private String name;
    private String description;
    private String mainImage;
    private ProductStatus status;
    private List<VariantCreateRequest> variants;
}
