package com.quickshop.notification.repository;

import com.quickshop.notification.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findAllByOrderBySentAtDesc();
    List<EmailLog> findByRecipientEmailOrderBySentAtDesc(String recipientEmail);
}
