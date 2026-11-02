package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.ReviewCreateRequest;
import com.quickshop.catalog.dto.ReviewDto;
import com.quickshop.catalog.entity.ProductReview;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductReviewRepository;
import com.quickshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CatalogCacheService cacheService;

    public List<ReviewDto> getReviewsByProduct(Long productId) {
        List<ProductReview> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return reviews.stream().map(r -> ReviewDto.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .subOrderId(r.getSubOrderId())
                .userId(r.getUserId())
                .userName(r.getUserName() != null ? r.getUserName() : "Khách hàng QuickShop")
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public ReviewDto addReview(Long productId, ReviewCreateRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        ProductReview review = ProductReview.builder()
                .productId(productId)
                .subOrderId(request.getSubOrderId() != null ? request.getSubOrderId() : 0L)
                .userId(request.getUserId() != null ? request.getUserId() : 1L)
                .userName(request.getUserName() != null && !request.getUserName().isBlank() ? request.getUserName() : "Khách hàng QuickShop")
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        ProductReview saved = reviewRepository.save(review);
        cacheService.evictProductCaches(productId);

        return ReviewDto.builder()
                .id(saved.getId())
                .productId(saved.getProductId())
                .subOrderId(saved.getSubOrderId())
                .userId(saved.getUserId())
                .userName(saved.getUserName())
                .rating(saved.getRating())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
