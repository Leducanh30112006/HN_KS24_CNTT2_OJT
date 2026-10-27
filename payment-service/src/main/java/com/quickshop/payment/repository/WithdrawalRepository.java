package com.quickshop.payment.repository;

import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.entity.enums.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    List<Withdrawal> findByShopId(Long shopId);

    List<Withdrawal> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<Withdrawal> findByStatus(WithdrawalStatus status);

    Optional<Withdrawal> findByIdempotencyKey(String idempotencyKey);
}
