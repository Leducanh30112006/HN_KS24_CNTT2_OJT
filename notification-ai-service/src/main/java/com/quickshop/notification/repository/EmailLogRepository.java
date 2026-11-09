package com.quickshop.notification.repository;

import com.quickshop.notification.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByRecipientEmail(String recipientEmail);

    Page<EmailLog> findAllByOrderBySentAtDesc(Pageable pageable);

    Page<EmailLog> findByStatusOrderBySentAtDesc(String status, Pageable pageable);

    Page<EmailLog> findByTemplateNameOrderBySentAtDesc(String templateName, Pageable pageable);

    long countByStatus(String status);
}
