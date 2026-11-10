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

    List<LedgerEntry> findByTransactionId(String transactionId);

    List<LedgerEntry> findByTransactionType(String transactionType);

    List<LedgerEntry> findByAccountDebitOrAccountCredit(String accountDebit, String accountCredit);

    @Query("SELECT SUM(l.amount) FROM LedgerEntry l WHERE l.accountDebit = :account")
    BigDecimal sumDebitByAccount(@Param("account") String account);

    @Query("SELECT SUM(l.amount) FROM LedgerEntry l WHERE l.accountCredit = :account")
    BigDecimal sumCreditByAccount(@Param("account") String account);
}
