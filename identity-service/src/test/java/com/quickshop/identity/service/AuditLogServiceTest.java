package com.quickshop.identity.service;

import com.quickshop.identity.entity.AuditLog;
import com.quickshop.identity.repository.AuditLogRepository;
import com.quickshop.identity.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService - Quản lý và ghi nhận nhật ký kiểm toán hệ thống")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    @DisplayName("Ghi nhận AuditLog thành công (logSuccess)")
    void logSuccess_ShouldSaveAuditLog() {
        AuditLog saved = AuditLog.builder()
                .id(1L)
                .userId(10L)
                .username("admin")
                .action("SHOP_APPROVED")
                .entityName("Shop")
                .entityId(5L)
                .details("Admin duyệt shop")
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(saved);

        AuditLog result = auditLogService.logSuccess(10L, "admin", "SHOP_APPROVED", "Shop", 5L, "Admin duyệt shop");

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo("SHOP_APPROVED");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Ghi nhận AuditLog thất bại (logFailure)")
    void logFailure_ShouldSaveFailedStatus() {
        AuditLog saved = AuditLog.builder()
                .id(2L)
                .userId(10L)
                .username("attacker")
                .action("LOGIN_FAILED")
                .entityName("Auth")
                .details("Sai mật khẩu")
                .status("FAILED")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(saved);

        AuditLog result = auditLogService.logFailure(10L, "attacker", "LOGIN_FAILED", "Auth", null, "Sai mật khẩu");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Tra cứu AuditLog theo action có phân trang")
    void getAuditLogs_ByAction_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog logItem = AuditLog.builder().id(1L).action("SHOP_APPROVED").build();
        Page<AuditLog> page = new PageImpl<>(List.of(logItem));

        when(auditLogRepository.findByActionOrderByCreatedAtDesc("SHOP_APPROVED", pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs("SHOP_APPROVED", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository, times(1)).findByActionOrderByCreatedAtDesc("SHOP_APPROVED", pageable);
    }
}
