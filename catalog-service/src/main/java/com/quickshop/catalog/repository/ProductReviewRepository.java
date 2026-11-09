package com.quickshop.catalog.repository;

import com.quickshop.catalog.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);
    long countByProductId(Long productId);

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.productId = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    /**
     * Kiểm tra xem SubOrder đã từng đánh giá sản phẩm này chưa (chống đánh giá trùng lặp)
     */
    boolean existsByProductIdAndSubOrderId(Long productId, Long subOrderId);

    /**
     * Tra cứu bản ghi đánh giá theo sản phẩm và đơn hàng
     */
    Optional<ProductReview> findByProductIdAndSubOrderId(Long productId, Long subOrderId);

    /**
     * Lấy danh sách đánh giá của một người dùng
     */
    List<ProductReview> findByUserIdOrderByCreatedAtDesc(Long userId);
}
