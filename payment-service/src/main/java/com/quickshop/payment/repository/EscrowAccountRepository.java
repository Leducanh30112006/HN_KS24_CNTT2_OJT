package com.quickshop.payment.repository;

import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.enums.EscrowStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EscrowAccountRepository extends JpaRepository<EscrowAccount, Long> {

    Optional<EscrowAccount> findBySubOrderId(Long subOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EscrowAccount e WHERE e.subOrderId = :subOrderId")
    Optional<EscrowAccount> findBySubOrderIdForUpdate(@Param("subOrderId") Long subOrderId);

    List<EscrowAccount> findByMasterOrderId(Long masterOrderId);

    List<EscrowAccount> findByStatus(EscrowStatus status);

    @Query("SELECT e FROM EscrowAccount e WHERE e.status = 'HOLDING' AND e.isDisputed = false AND e.deliveredAt <= :thresholdTime")
    List<EscrowAccount> findEligibleForAutoRelease(@Param("thresholdTime") LocalDateTime thresholdTime);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EscrowAccount e WHERE e.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") EscrowStatus status);
}
