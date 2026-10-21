package com.quickshop.payment.repository;

import com.quickshop.payment.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByShopId(Long shopId);
    List<Withdrawal> findByStatus(String status);
}
