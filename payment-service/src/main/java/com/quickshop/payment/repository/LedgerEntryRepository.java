package com.quickshop.payment.repository;

import com.quickshop.payment.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    boolean existsByTransactionId(String transactionId);

    List<LedgerEntry> findByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.accountCredit = :account")
    BigDecimal sumAmountByAccountCredit(@Param("account") String account);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.accountDebit = :account")
    BigDecimal sumAmountByAccountDebit(@Param("account") String account);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l")
    BigDecimal sumTotalAmount();
}
