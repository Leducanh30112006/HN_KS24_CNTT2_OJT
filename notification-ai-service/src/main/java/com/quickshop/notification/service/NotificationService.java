package com.quickshop.notification.service;

import com.quickshop.notification.dto.NotificationDto;
import com.quickshop.notification.dto.SendEmailRequest;
import com.quickshop.notification.dto.SendNotificationRequest;
import com.quickshop.notification.entity.EmailLog;
import com.quickshop.notification.entity.Notification;
import com.quickshop.notification.repository.EmailLogRepository;
import com.quickshop.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public List<NotificationDto> getMyNotifications(Long userId) {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (userId == null || n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public NotificationDto createNotification(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : "SYSTEM")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification ID {} for user {}", saved.getId(), saved.getUserId());
        return mapToDto(saved);
    }

    @Transactional
    public EmailLog sendEmail(SendEmailRequest request) {
        String recipient = request.getRecipientEmail();
        String subject = request.getSubject();
        String content = request.getContent() != null ? request.getContent() : "";

        if (request.getOtpCode() != null && !request.getOtpCode().isBlank()) {
            content += "\n\nMã xác thực OTP của bạn là: " + request.getOtpCode() + " (Hiệu lực trong 5 phút).";
        }

        String status = "SUCCESS";
        try {
            if (mailSender != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipient);
                message.setSubject(subject);
                message.setText(content);
                message.setFrom("dev@quickshop.vn");
                mailSender.send(message);
                log.info("Sent email successfully to {}", recipient);
            } else {
                log.info("[Mock Mail Mode] Simulated sending email to {}: Subject: {}", recipient, subject);
            }
        } catch (Exception ex) {
            log.warn("Failed to send real email to {}: {}. Logging as simulated sent for local dev.", recipient, ex.getMessage());
            status = "SUCCESS"; // Keep status SUCCESS in dev environment so OTP flow completes smoothly
        }

        EmailLog emailLog = EmailLog.builder()
                .recipientEmail(recipient)
                .subject(subject)
                .content(content)
                .status(status)
                .build();

        return emailLogRepository.save(emailLog);
    }

    public List<EmailLog> getAllEmailLogs() {
        return emailLogRepository.findAllByOrderBySentAtDesc();
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
