package com.quickshop.order.repository;

import com.quickshop.order.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    List<VoucherUsage> findByUserId(Long userId);
    List<VoucherUsage> findByMasterOrderId(Long masterOrderId);
    List<VoucherUsage> findByVoucherId(Long voucherId);
    long countByVoucherIdAndUserId(Long voucherId, Long userId);
}
