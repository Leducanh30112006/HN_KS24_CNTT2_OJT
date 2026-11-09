package com.quickshop.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "email_logs")
@EntityListeners(AuditingEntityListener.class)
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(name = "template_name", length = 50)
    private String templateName; // ORDER_CONFIRMATION, SHOP_APPROVED, SHOP_REJECTED, RESET_PASSWORD

    @Column(name = "content_preview", columnDefinition = "TEXT")
    private String contentPreview;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILED, RETRYING

    @CreatedDate
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;
}
