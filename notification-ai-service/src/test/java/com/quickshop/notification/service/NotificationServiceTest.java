package com.quickshop.notification.service;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.common.event.ShopApprovedEvent;
import com.quickshop.common.event.ShopRejectedEvent;
import com.quickshop.notification.entity.EmailLog;
import com.quickshop.notification.entity.Notification;
import com.quickshop.notification.repository.EmailLogRepository;
import com.quickshop.notification.repository.NotificationRepository;
import com.quickshop.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService - Quản lý thông báo in-app và ghi nhận email logs")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailLogRepository emailLogRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("Tạo thông báo in-app thành công")
    void createNotification_Success() {
        Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .title("Thông báo mới")
                .content("Nội dung thông báo")
                .isRead(false)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        Notification result = notificationService.createNotification(10L, "Thông báo mới", "Nội dung thông báo");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Thông báo mới");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Ghi nhận nhật ký email thành công (logEmail)")
    void logEmail_Success() {
        EmailLog emailLog = EmailLog.builder()
                .id(1L)
                .recipientEmail("user@quickshop.vn")
                .subject("Tiêu đề")
                .templateName("ORDER_CONFIRMATION")
                .status("SUCCESS")
                .build();

        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);

        EmailLog result = notificationService.logEmail("user@quickshop.vn", "Tiêu đề", "ORDER_CONFIRMATION", "SUCCESS", "Preview", null);

        assertThat(result).isNotNull();
        assertThat(result.getRecipientEmail()).isEqualTo("user@quickshop.vn");
        verify(emailLogRepository, times(1)).save(any(EmailLog.class));
    }

    @Test
    @DisplayName("Xử lý sự kiện OrderCreatedEvent - Tạo thông báo và email xác nhận cho khách hàng")
    void processOrderCreated_Success() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("ORD-123456")
                .customerId(10L)
                .customerEmail("customer@quickshop.vn")
                .totalAmount(BigDecimal.valueOf(500000))
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.processOrderCreated(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(emailLogRepository, times(1)).save(argThat(log ->
                "customer@quickshop.vn".equals(log.getRecipientEmail()) &&
                "ORDER_CONFIRMATION".equals(log.getTemplateName())
        ));
    }

    @Test
    @DisplayName("Xử lý sự kiện ShopApprovedEvent - Tạo thông báo chúc mừng và email cho chủ shop")
    void processShopApproved_Success() {
        ShopApprovedEvent event = ShopApprovedEvent.builder()
                .shopId(5L)
                .ownerId(20L)
                .shopName("TechStore")
                .ownerEmail("owner@quickshop.vn")
                .approvedAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.processShopApproved(event);

        verify(notificationRepository, times(1)).save(argThat(n ->
                n.getUserId().equals(20L) && n.getContent().contains("TechStore")
        ));
        verify(emailLogRepository, times(1)).save(argThat(log ->
                "owner@quickshop.vn".equals(log.getRecipientEmail()) &&
                "SHOP_APPROVED".equals(log.getTemplateName())
        ));
    }

    @Test
    @DisplayName("Xử lý sự kiện ShopRejectedEvent - Tạo thông báo từ chối kèm lý do và email")
    void processShopRejected_Success() {
        ShopRejectedEvent event = ShopRejectedEvent.builder()
                .shopId(5L)
                .ownerId(20L)
                .shopName("TechStore")
                .ownerEmail("owner@quickshop.vn")
                .reason("CCCD mờ không đọc được")
                .rejectedAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.processShopRejected(event);

        verify(notificationRepository, times(1)).save(argThat(n ->
                n.getUserId().equals(20L) && n.getContent().contains("CCCD mờ")
        ));
        verify(emailLogRepository, times(1)).save(argThat(log ->
                "owner@quickshop.vn".equals(log.getRecipientEmail()) &&
                "SHOP_REJECTED".equals(log.getTemplateName())
        ));
    }

    @Test
    @DisplayName("Đánh dấu thông báo đã đọc (markAsRead)")
    void markAsRead_Success() {
        Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .title("Thông báo")
                .content("Nội dung")
                .isRead(false)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.markAsRead(1L, 10L);

        assertThat(result.isRead()).isTrue();
        verify(notificationRepository, times(1)).save(notification);
    }
}
