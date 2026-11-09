package com.quickshop.notification.service;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.common.event.ShopApprovedEvent;
import com.quickshop.common.event.ShopRejectedEvent;
import com.quickshop.notification.entity.EmailLog;
import com.quickshop.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    Notification createNotification(Long userId, String title, String content);

    EmailLog logEmail(String recipientEmail, String subject, String templateName, String status, String contentPreview, String errorMessage);

    void processOrderCreated(OrderCreatedEvent event);

    void processShopApproved(ShopApprovedEvent event);

    void processShopRejected(ShopRejectedEvent event);

    List<Notification> getNotificationsForUser(Long userId);

    Notification markAsRead(Long notificationId, Long userId);

    Page<EmailLog> getEmailLogs(String status, Pageable pageable);
}
