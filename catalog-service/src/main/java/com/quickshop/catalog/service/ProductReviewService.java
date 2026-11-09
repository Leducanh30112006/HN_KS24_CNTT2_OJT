package com.quickshop.catalog.service;

import com.quickshop.catalog.client.OrderServiceClient;
import com.quickshop.catalog.dto.ReviewCreateRequest;
import com.quickshop.catalog.dto.ReviewDto;
import com.quickshop.catalog.entity.ProductReview;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductReviewRepository;
import com.quickshop.common.exception.BadRequestException;
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
    private final OrderServiceClient orderServiceClient;

    /**
     * Lấy danh sách đánh giá & nhận xét của sản phẩm
     */
    public List<ReviewDto> getReviewsByProduct(Long productId) {
        List<ProductReview> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return reviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Khách hàng gửi đánh giá 1-5 sao và nhận xét cho sản phẩm
     * Điều kiện nghiệp vụ:
     * 1. Sản phẩm phải tồn tại
     * 2. Điểm đánh giá phải từ 1 đến 5 sao
     * 3. Phải cung cấp mã đơn hàng con (subOrderId > 0)
     * 4. Mỗi đơn hàng chỉ được gửi đánh giá đúng 1 lần cho cùng 1 sản phẩm (Validate một đánh giá/đơn)
     * 5. Chỉ cho phép đánh giá sau khi đơn hàng đã đạt trạng thái COMPLETED (Order Contract)
     * 6. Sau khi tạo đánh giá thành công, tự động invalidate Redis Cache sản phẩm để cập nhật rating trung bình
     */
    @Transactional
    public ReviewDto addReview(Long productId, ReviewCreateRequest request) {
        // 1. Kiểm tra sản phẩm tồn tại
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        // 2. Kiểm tra rating 1 - 5 sao
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Số sao đánh giá phải từ 1 đến 5 sao");
        }

        // 3. Kiểm tra bắt buộc có subOrderId
        if (request.getSubOrderId() == null || request.getSubOrderId() <= 0) {
            throw new BadRequestException("Mã đơn hàng (subOrderId) là bắt buộc để đánh giá sản phẩm");
        }

        // 4. Validate một đánh giá/đơn: Chống đánh giá trùng lặp cho cùng một đơn hàng
        if (reviewRepository.existsByProductIdAndSubOrderId(productId, request.getSubOrderId())) {
            throw new BadRequestException("Đơn hàng #" + request.getSubOrderId() + " đã gửi đánh giá cho sản phẩm này rồi");
        }

        // 5. Kiểm tra trạng thái đơn hàng: Chỉ cho phép đánh giá khi đơn hàng ở trạng thái COMPLETED
        boolean isEligible = orderServiceClient.isSubOrderCompleted(request.getSubOrderId(), request.getUserId(), productId);
        if (!isEligible) {
            throw new BadRequestException("Chỉ được phép đánh giá sau khi đơn hàng đã hoàn thành (COMPLETED)");
        }

        // 6. Lưu bản ghi đánh giá
        ProductReview review = ProductReview.builder()
                .productId(productId)
                .subOrderId(request.getSubOrderId())
                .userId(request.getUserId() != null ? request.getUserId() : 1L)
                .userName(request.getUserName() != null && !request.getUserName().isBlank() 
                        ? request.getUserName() 
                        : "Khách hàng QuickShop")
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .build();

        ProductReview saved = reviewRepository.save(review);
        log.info("Lưu đánh giá thành công cho sản phẩm #{}, đơn hàng #{}, rating: {} sao", 
                productId, request.getSubOrderId(), request.getRating());

        // 7. Invalidate Redis Cache sản phẩm và danh sách lọc
        cacheService.evictProductCaches(productId);

        return mapToDto(saved);
    }

    /**
     * Tính toán điểm đánh giá trung bình của sản phẩm (làm tròn 1 chữ số thập phân)
     */
    public Double getAverageRating(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        if (avg == null) {
            return 5.0; // Mặc định 5 sao cho sản phẩm mới chưa có đánh giá
        }
        return Math.round(avg * 10.0) / 10.0;
    }

    /**
     * Đếm tổng số lượt đánh giá của sản phẩm
     */
    public long getReviewCount(Long productId) {
        return reviewRepository.countByProductId(productId);
    }

    private ReviewDto mapToDto(ProductReview r) {
        return ReviewDto.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .subOrderId(r.getSubOrderId())
                .userId(r.getUserId())
                .userName(r.getUserName() != null ? r.getUserName() : "Khách hàng QuickShop")
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
