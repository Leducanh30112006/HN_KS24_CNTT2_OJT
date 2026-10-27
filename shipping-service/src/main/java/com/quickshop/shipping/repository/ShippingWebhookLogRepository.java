package com.quickshop.shipping.repository;

import com.quickshop.shipping.entity.ShippingWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingWebhookLogRepository extends JpaRepository<ShippingWebhookLog, Long> {

    Optional<ShippingWebhookLog> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
