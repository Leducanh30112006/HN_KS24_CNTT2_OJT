package com.quickshop.order.repository;

import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.SubOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {
    List<SubOrder> findByMasterOrderId(Long masterOrderId);
    List<SubOrder> findByShopId(Long shopId);
    List<SubOrder> findByStatus(SubOrderStatus status);
}
