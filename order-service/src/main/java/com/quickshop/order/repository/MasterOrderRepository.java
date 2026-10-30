package com.quickshop.order.repository;

import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.MasterOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterOrderRepository extends JpaRepository<MasterOrder, Long> {
    List<MasterOrder> findByUserId(Long userId);
    List<MasterOrder> findByStatus(MasterOrderStatus status);
}
