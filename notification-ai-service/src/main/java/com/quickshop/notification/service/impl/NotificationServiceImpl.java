package com.quickshop.notification.service.impl;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.common.event.ShopApprovedEvent;
import com.quickshop.common.event.ShopRejectedEvent;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.notification.entity.EmailLog;
import com.quickshop.notification.entity.Notification;
import com.quickshop.notification.repository.EmailLogRepository;
import com.quickshop.notification.repository.NotificationRepository;
import com.quickshop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;

    @Override
    @Transactional
    public Notification createNotification(Long userId, String title, String content) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification [id={}] for userId: {}", saved.getId(), userId);
        return saved;
    }

    @Override
    @Transactional
    public EmailLog logEmail(String recipientEmail, String subject, String templateName, String status, String contentPreview, String errorMessage) {
        EmailLog emailLog = EmailLog.builder()
                .recipientEmail(recipientEmail)
                .subject(subject)
                .templateName(templateName)
                .contentPreview(contentPreview)
                .status(status != null ? status : "SUCCESS")
                .errorMessage(errorMessage)
                .retryCount(0)
                .build();
        EmailLog saved = emailLogRepository.save(emailLog);
        log.info("Recorded EmailLog [id={}, recipient={}, template={}, status={}]", saved.getId(), recipientEmail, templateName, status);
        return saved;
    }

    @Override
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreated notification for orderId: {}, customerId: {}", event.getOrderId(), event.getCustomerId());

        String title = "Đặt hàng thành công!";
        String content = String.format("Đơn hàng #%s đã được ghi nhận thành công với tổng số tiền %s đ. Người bán đang xử lý đóng gói hàng.",
                event.getOrderId(),
                event.getTotalAmount() != null ? event.getTotalAmount().toPlainString() : "0");

        if (event.getCustomerId() != null) {
            createNotification(event.getCustomerId(), title, content);
        }

        String recipientEmail = StringUtils.hasText(event.getCustomerEmail()) ? event.getCustomerEmail() : "customer_" + event.getCustomerId() + "@quickshop.vn";
        logEmail(recipientEmail, "QuickShop - Xác nhận đơn hàng #" + event.getOrderId(), "ORDER_CONFIRMATION", "SUCCESS", content, null);
    }

    @Override
    @Transactional
    public void processShopApproved(ShopApprovedEvent event) {
        log.info("Processing ShopApproved notification for shopId: {}, ownerId: {}", event.getShopId(), event.getOwnerId());

        String title = "Gian hàng đã được phê duyệt!";
        String content = String.format("Chúc mừng! Gian hàng '%s' của bạn đã được phê duyệt thành công. Bạn đã được nâng cấp quyền Người bán (ROLE_SELLER) và có thể bắt đầu đăng bán sản phẩm.",
                event.getShopName());

        if (event.getOwnerId() != null) {
            createNotification(event.getOwnerId(), title, content);
        }

        String recipientEmail = StringUtils.hasText(event.getOwnerEmail()) ? event.getOwnerEmail() : "seller_" + event.getOwnerId() + "@quickshop.vn";
        logEmail(recipientEmail, "QuickShop - Chúc mừng gian hàng '" + event.getShopName() + "' được phê duyệt", "SHOP_APPROVED", "SUCCESS", content, null);
    }

    @Override
    @Transactional
    public void processShopRejected(ShopRejectedEvent event) {
        log.info("Processing ShopRejected notification for shopId: {}, ownerId: {}", event.getShopId(), event.getOwnerId());

        String reason = StringUtils.hasText(event.getReason()) ? event.getReason() : "Chưa đạt tiêu chuẩn xét duyệt gian hàng";
        String title = "Hồ sơ gian hàng chưa được duyệt";
        String content = String.format("Rất tiếc! Hồ sơ gian hàng '%s' của bạn chưa được phê duyệt. Lý do: %s. Vui lòng cập nhật lại hồ sơ để được xét duyệt lại.",
                event.getShopName(), reason);

        if (event.getOwnerId() != null) {
            createNotification(event.getOwnerId(), title, content);
        }

        String recipientEmail = StringUtils.hasText(event.getOwnerEmail()) ? event.getOwnerEmail() : "seller_" + event.getOwnerId() + "@quickshop.vn";
        logEmail(recipientEmail, "QuickShop - Thông báo kết quả xét duyệt gian hàng '" + event.getShopName() + "'", "SHOP_REJECTED", "SUCCESS", content, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (userId != null && !notification.getUserId().equals(userId)) {
            log.warn("User {} attempted to mark notification {} belonging to user {}", userId, notificationId, notification.getUserId());
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailLog> getEmailLogs(String status, Pageable pageable) {
        if (StringUtils.hasText(status)) {
            return emailLogRepository.findByStatusOrderBySentAtDesc(status.trim().toUpperCase(), pageable);
        }
        return emailLogRepository.findAllByOrderBySentAtDesc(pageable);
    }
}
