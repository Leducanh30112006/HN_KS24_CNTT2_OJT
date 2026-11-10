package com.quickshop.payment.repository;

import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    Optional<PaymentTransaction> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);

    List<PaymentTransaction> findByOrderId(String orderId);

    List<PaymentTransaction> findByUserId(Long userId);

    List<PaymentTransaction> findByStatus(PaymentStatus status);
}
