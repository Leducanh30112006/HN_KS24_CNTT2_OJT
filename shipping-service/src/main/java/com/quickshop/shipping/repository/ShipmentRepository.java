package com.quickshop.shipping.repository;

import com.quickshop.shipping.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingCode(String trackingCode);
    Optional<Shipment> findBySubOrderId(Long subOrderId);
}
