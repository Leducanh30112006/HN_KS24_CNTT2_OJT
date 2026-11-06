package com.quickshop.order.repository;

import com.quickshop.order.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);
    Optional<Voucher> findByCodeAndStatus(String code, String status);
    boolean existsByCode(String code);
    List<Voucher> findByShopIdAndStatus(Long shopId, String status);
    List<Voucher> findByShopIdIsNullAndStatus(String status);
}
