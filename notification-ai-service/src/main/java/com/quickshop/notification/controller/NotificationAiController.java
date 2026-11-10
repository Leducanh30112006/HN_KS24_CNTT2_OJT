package com.quickshop.notification.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.notification.entity.EmailLog;
import com.quickshop.notification.entity.Notification;
import com.quickshop.notification.repository.EmailLogRepository;
import com.quickshop.notification.repository.NotificationRepository;
import com.quickshop.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Notification & AI Assistant", description = "Quản lý thông báo in-app, nhật ký email và trợ lý AI")
public class NotificationAiController {

    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;
    private final NotificationService notificationService;

    @GetMapping("/api/notifications/my")
    @Operation(summary = "Lấy danh sách thông báo của tài khoản hiện tại")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getNotificationsForUser(userId)));
    }

    @PutMapping("/api/notifications/{id}/read")
    @Operation(summary = "Đánh dấu thông báo đã đọc")
    public ResponseEntity<ApiResponse<Notification>> markNotificationAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu thông báo là đã đọc", notificationService.markAsRead(id, userId)));
    }

    @GetMapping("/api/notifications/email-logs")
    @Operation(summary = "Tra cứu danh sách email logs (phân trang và lọc theo status)")
    public ResponseEntity<ApiResponse<Page<EmailLog>>> getEmailLogs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getEmailLogs(status, pageable)));
    }

    @GetMapping("/api/notifications/stats")
    @Operation(summary = "Thống kê tổng quan số lượng thông báo và email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationStats(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("unreadNotifications", notificationRepository.countByUserIdAndIsReadFalse(userId));
        stats.put("totalNotifications", notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).size());
        stats.put("successfulEmails", emailLogRepository.countByStatus("SUCCESS"));
        stats.put("failedEmails", emailLogRepository.countByStatus("FAILED"));
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
