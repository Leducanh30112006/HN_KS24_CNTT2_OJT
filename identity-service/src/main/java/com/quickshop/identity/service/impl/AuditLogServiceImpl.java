package com.quickshop.identity.service.impl;

import com.quickshop.identity.entity.AuditLog;
import com.quickshop.identity.repository.AuditLogRepository;
import com.quickshop.identity.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logAction(Long userId, String username, String action, String entityName, Long entityId, String details, String ipAddress, String status) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .status(status != null ? status : "SUCCESS")
                    .build();

            AuditLog saved = auditLogRepository.save(auditLog);
            log.info("AuditLog recorded: action={}, entity={}/{}, user={}/{}", action, entityName, entityId, userId, username);
            return saved;
        } catch (Exception ex) {
            log.error("Failed to save audit log: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logSuccess(Long userId, String username, String action, String entityName, Long entityId, String details) {
        return logAction(userId, username, action, entityName, entityId, details, null, "SUCCESS");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logFailure(Long userId, String username, String action, String entityName, Long entityId, String details) {
        return logAction(userId, username, action, entityName, entityId, details, null, "FAILED");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(String action, Long userId, Pageable pageable) {
        if (StringUtils.hasText(action)) {
            return auditLogRepository.findByActionOrderByCreatedAtDesc(action.trim().toUpperCase(), pageable);
        }
        if (userId != null) {
            return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 100));
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
    }
}
