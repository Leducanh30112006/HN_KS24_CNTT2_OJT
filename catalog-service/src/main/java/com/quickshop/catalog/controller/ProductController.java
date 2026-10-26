package com.quickshop.catalog.controller;

import com.quickshop.catalog.entity.Category;
import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.repository.CategoryRepository;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductVariantRepository;
import com.quickshop.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryRepository.findAll()));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<Product>>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long shopId
    ) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (shopId != null) {
            products = productRepository.findByShopId(shopId);
        } else {
            products = productRepository.findByStatus(ProductStatus.ACTIVE);
            if (products.isEmpty()) {
                products = productRepository.findAll();
            }
        }
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductDetails(@PathVariable Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOpt.get();
        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("variants", variants);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
