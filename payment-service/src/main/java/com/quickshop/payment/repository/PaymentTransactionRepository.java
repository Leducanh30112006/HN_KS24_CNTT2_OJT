package com.quickshop.payment.repository;

import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    List<PaymentTransaction> findByOrderId(Long orderId);

    Optional<PaymentTransaction> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentTransaction> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
