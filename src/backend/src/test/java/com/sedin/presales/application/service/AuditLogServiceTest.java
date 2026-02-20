package com.sedin.presales.application.service;

import com.sedin.presales.domain.entity.AuditLog;
import com.sedin.presales.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    @DisplayName("log should save audit log with all fields")
    void log_shouldSaveAuditLogWithAllFields() {
        String userEmail = "admin@sedin.com";
        String action = "CREATE_DOCUMENT";
        String resourceType = "DOCUMENT";
        UUID resourceId = UUID.randomUUID();
        String ipAddress = "192.168.1.1";
        String details = "{\"title\":\"Test Document\"}";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log(userEmail, action, resourceType, resourceId, ipAddress, details);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUserEmail()).isEqualTo(userEmail);
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getResourceType()).isEqualTo(resourceType);
        assertThat(saved.getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getIpAddress()).isEqualTo(ipAddress);
        assertThat(saved.getDetails()).isEqualTo(details);
    }

    @Test
    @DisplayName("log should save audit log with null resourceId for create operations")
    void log_shouldSaveAuditLogWithNullResourceId() {
        String userEmail = "editor@sedin.com";
        String action = "UPLOAD_DOCUMENT";
        String resourceType = "DOCUMENT";
        String ipAddress = "10.0.0.1";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log(userEmail, action, resourceType, null, ipAddress, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUserEmail()).isEqualTo(userEmail);
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getResourceType()).isEqualTo(resourceType);
        assertThat(saved.getResourceId()).isNull();
        assertThat(saved.getDetails()).isNull();
    }

    @Test
    @DisplayName("log should save audit log with null details")
    void log_shouldSaveAuditLogWithNullDetails() {
        String userEmail = "viewer@sedin.com";
        String action = "VIEW_DOCUMENT";
        String resourceType = "DOCUMENT";
        UUID resourceId = UUID.randomUUID();
        String ipAddress = "172.16.0.1";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log(userEmail, action, resourceType, resourceId, ipAddress, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUserEmail()).isEqualTo(userEmail);
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getDetails()).isNull();
    }
}
