package com.quickshop.notification.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.notification.dto.NotificationDto;
import com.quickshop.notification.dto.SendEmailRequest;
import com.quickshop.notification.dto.SendNotificationRequest;
import com.quickshop.notification.entity.EmailLog;
import com.quickshop.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification & Messaging Service", description = "Quản lý thông báo trong ứng dụng & gửi email OTP / Đơn hàng")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread-count")
    @Operation(summary = "Lấy số lượng thông báo chưa đọc")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId
    ) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Thành công", Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo là đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId
    ) {
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc thông báo", null));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo là đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId
    ) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc tất cả thông báo", null));
    }

    @PostMapping("/send")
    @Operation(summary = "Tạo thông báo mới cho người dùng (Admin hoặc Microservice)")
    public ResponseEntity<ApiResponse<NotificationDto>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        NotificationDto result = notificationService.createNotification(request);
        return ResponseEntity.ok(ApiResponse.created("Tạo thông báo thành công", result));
    }

    @PostMapping("/send-email")
    @Operation(summary = "Gửi email trực tiếp hoặc qua OTP Mailer")
    public ResponseEntity<ApiResponse<EmailLog>> sendEmail(
            @Valid @RequestBody SendEmailRequest request
    ) {
        EmailLog log = notificationService.sendEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Đã tiếp nhận yêu cầu gửi email", log));
    }

    @GetMapping("/email-logs")
    @Operation(summary = "Admin xem lịch sử email đã gửi")
    public ResponseEntity<ApiResponse<List<EmailLog>>> getEmailLogs() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách log email thành công", notificationService.getAllEmailLogs()));
    }
}
