package com.quickshop.identity.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.identity.entity.AuditLog;
import com.quickshop.identity.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin Audit Log", description = "Quản lý và tra cứu nhật ký kiểm toán hệ thống (Audit Logs)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Admin tra cứu nhật ký kiểm toán hệ thống (phân trang và lọc theo action, userId)")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AuditLog> logs = auditLogService.getAuditLogs(action, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    @GetMapping("/recent")
    @Operation(summary = "Lấy danh sách nhật ký kiểm toán gần đây nhất")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getRecentLogs(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getRecentLogs(limit)));
    }
}
