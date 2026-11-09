package com.quickshop.catalog.service;

import com.quickshop.catalog.client.OrderServiceClient;
import com.quickshop.catalog.dto.ReviewCreateRequest;
import com.quickshop.catalog.dto.ReviewDto;
import com.quickshop.catalog.entity.ProductReview;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductReviewRepository;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewService - Đánh giá 1-5 sao sau đơn COMPLETED, Chống trùng lặp & Tính Rating trung bình")
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CatalogCacheService cacheService;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private ProductReviewService reviewService;

    private final Long productId = 100L;
    private final Long subOrderId = 501L;
    private final Long userId = 10L;

    private ReviewCreateRequest validRequest;
    private ProductReview sampleReview;

    @BeforeEach
    void setUp() {
        validRequest = ReviewCreateRequest.builder()
                .subOrderId(subOrderId)
                .userId(userId)
                .userName("Lê Văn Mua")
                .rating(5)
                .comment("Sản phẩm tuyệt vời, giao hàng cực nhanh đóng gói cẩn thận!")
                .build();

        sampleReview = ProductReview.builder()
                .id(1L)
                .productId(productId)
                .subOrderId(subOrderId)
                .userId(userId)
                .userName("Lê Văn Mua")
                .rating(5)
                .comment("Sản phẩm tuyệt vời, giao hàng cực nhanh đóng gói cẩn thận!")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("1. Gửi đánh giá sản phẩm (Add Review)")
    class AddReviewTests {

        @Test
        @DisplayName("Gửi đánh giá thành công khi đơn hàng COMPLETED, đủ 1-5 sao và chưa từng đánh giá")
        void addReview_Success() {
            when(productRepository.existsById(productId)).thenReturn(true);
            when(reviewRepository.existsByProductIdAndSubOrderId(productId, subOrderId)).thenReturn(false);
            when(orderServiceClient.isSubOrderCompleted(subOrderId, userId, productId)).thenReturn(true);
            when(reviewRepository.save(any(ProductReview.class))).thenReturn(sampleReview);

            ReviewDto result = reviewService.addReview(productId, validRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getProductId()).isEqualTo(productId);
            assertThat(result.getSubOrderId()).isEqualTo(subOrderId);
            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getComment()).contains("Sản phẩm tuyệt vời");

            verify(reviewRepository).save(any(ProductReview.class));
            verify(cacheService).evictProductCaches(productId);
        }

        @Test
        @DisplayName("Từ chối khi sản phẩm không tồn tại trong hệ thống (ResourceNotFoundException)")
        void addReview_ProductNotFound_ThrowsException() {
            when(productRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.addReview(999L, validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");

            verify(reviewRepository, never()).save(any());
            verify(cacheService, never()).evictProductCaches(any());
        }

        @Test
        @DisplayName("Từ chối khi số sao đánh giá nhỏ hơn 1 (rating < 1)")
        void addReview_RatingLessThan1_ThrowsBadRequest() {
            when(productRepository.existsById(productId)).thenReturn(true);
            validRequest.setRating(0);

            assertThatThrownBy(() -> reviewService.addReview(productId, validRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số sao đánh giá phải từ 1 đến 5 sao");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Từ chối khi số sao đánh giá lớn hơn 5 (rating > 5)")
        void addReview_RatingGreaterThan5_ThrowsBadRequest() {
            when(productRepository.existsById(productId)).thenReturn(true);
            validRequest.setRating(6);

            assertThatThrownBy(() -> reviewService.addReview(productId, validRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số sao đánh giá phải từ 1 đến 5 sao");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Từ chối khi thiếu mã đơn hàng (subOrderId là null hoặc <= 0)")
        void addReview_MissingSubOrderId_ThrowsBadRequest() {
            when(productRepository.existsById(productId)).thenReturn(true);
            validRequest.setSubOrderId(null);

            assertThatThrownBy(() -> reviewService.addReview(productId, validRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Mã đơn hàng (subOrderId) là bắt buộc");

            validRequest.setSubOrderId(0L);
            assertThatThrownBy(() -> reviewService.addReview(productId, validRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Mã đơn hàng (subOrderId) là bắt buộc");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Validate một đánh giá/đơn: Chống đánh giá trùng lặp nếu đơn hàng đã từng đánh giá sản phẩm này")
        void addReview_AlreadyReviewed_ThrowsBadRequest() {
            when(productRepository.existsById(productId)).thenReturn(true);
            when(reviewRepository.existsByProductIdAndSubOrderId(productId, subOrderId)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.addReview(productId, validRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("đã gửi đánh giá cho sản phẩm này rồi");

            verify(orderServiceClient, never()).isSubOrderCompleted(any(), any(), any());
            verify(reviewRepository, never()).save(any());
            verify(cacheService, never()).evictProductCaches(any());
        }

        @Test
        @DisplayName("Chỉ cho phép đánh giá sau khi đơn hàng COMPLETED: Từ chối nếu đơn chưa hoàn thành")
        void addReview_OrderNotCompleted_ThrowsBadRequest() {
            when(productRepository.existsById(productId)).thenReturn(true);
            when(reviewRepository.existsByProductIdAndSubOrderId(productId, subOrderId)).thenReturn(false);
            when(orderServiceClient.isSubOrderCompleted(subOrderId, userId, productId)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.addReview(productId, validRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Chỉ được phép đánh giá sau khi đơn hàng đã hoàn thành (COMPLETED)");

            verify(reviewRepository, never()).save(any());
            verify(cacheService, never()).evictProductCaches(any());
        }
    }

    @Nested
    @DisplayName("2. Tính toán Rating trung bình và Danh sách Đánh giá")
    class RatingCalculationTests {

        @Test
        @DisplayName("getAverageRating tính đúng và làm tròn 1 chữ số thập phân")
        void getAverageRating_CalculatesAndRoundsCorrectly() {
            when(reviewRepository.getAverageRatingByProductId(productId)).thenReturn(4.6666667);

            Double avg = reviewService.getAverageRating(productId);

            assertThat(avg).isEqualTo(4.7);
        }

        @Test
        @DisplayName("getAverageRating mặc định trả về 5.0 khi sản phẩm chưa có đánh giá nào")
        void getAverageRating_NoReviews_DefaultsTo5() {
            when(reviewRepository.getAverageRatingByProductId(productId)).thenReturn(null);

            Double avg = reviewService.getAverageRating(productId);

            assertThat(avg).isEqualTo(5.0);
        }

        @Test
        @DisplayName("getReviewCount trả về chính xác số lượng đánh giá")
        void getReviewCount_ReturnsCount() {
            when(reviewRepository.countByProductId(productId)).thenReturn(42L);

            long count = reviewService.getReviewCount(productId);

            assertThat(count).isEqualTo(42L);
        }

        @Test
        @DisplayName("getReviewsByProduct trả về danh sách ReviewDto theo thứ tự mới nhất")
        void getReviewsByProduct_ReturnsList() {
            when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId))
                    .thenReturn(List.of(sampleReview));

            List<ReviewDto> list = reviewService.getReviewsByProduct(productId);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getRating()).isEqualTo(5);
            assertThat(list.get(0).getUserName()).isEqualTo("Lê Văn Mua");
        }
    }
}
