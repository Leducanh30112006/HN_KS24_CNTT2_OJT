package com.quickshop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto implements Serializable {
    private Long id;
    private Long parentId;
    private String parentName;
    private String name;
    private String slug;
    private String icon;
    private Long productCount;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<CategoryDto> children = new ArrayList<>();
}
