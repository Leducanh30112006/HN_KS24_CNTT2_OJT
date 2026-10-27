package com.quickshop.shipping.repository;

import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.entity.enums.ShippingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findBySubOrderId(Long subOrderId);

    Optional<Shipment> findByTrackingCode(String trackingCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shipment s WHERE s.trackingCode = :trackingCode")
    Optional<Shipment> findByTrackingCodeForUpdate(@Param("trackingCode") String trackingCode);

    boolean existsBySubOrderId(Long subOrderId);

    boolean existsByTrackingCode(String trackingCode);

    List<Shipment> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<Shipment> findByShippingStatus(ShippingStatus shippingStatus);
}
