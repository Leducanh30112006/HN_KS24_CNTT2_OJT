package com.quickshop.identity.service;

import com.quickshop.identity.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditLogService {

    AuditLog logAction(Long userId, String username, String action, String entityName, Long entityId, String details, String ipAddress, String status);

    AuditLog logSuccess(Long userId, String username, String action, String entityName, Long entityId, String details);

    AuditLog logFailure(Long userId, String username, String action, String entityName, Long entityId, String details);

    Page<AuditLog> getAuditLogs(String action, Long userId, Pageable pageable);

    List<AuditLog> getRecentLogs(int limit);
}
