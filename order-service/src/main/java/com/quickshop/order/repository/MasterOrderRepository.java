package com.quickshop.order.repository;

import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MasterOrderRepository extends JpaRepository<MasterOrder, Long> {

    List<MasterOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Tìm các đơn hàng ở các trạng thái chỉ định (ví dụ DRAFT, PENDING_PAYMENT)
     * được tạo trước thời điểm cutoffTime (dùng để quét đơn quá hạn 15 phút chưa thanh toán).
     */
    @Query("SELECT m FROM MasterOrder m WHERE m.status IN :statuses AND m.createdAt <= :cutoffTime")
    List<MasterOrder> findExpiredOrders(@Param("statuses") List<MasterOrderStatus> statuses,
                                        @Param("cutoffTime") LocalDateTime cutoffTime);
}
