package com.sedin.presales.application.service;

import com.sedin.presales.domain.entity.AuditLog;
import com.sedin.presales.domain.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists an audit log entry in its own transaction so it commits independently
     * of the calling transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String userEmail, String action, String resourceType, UUID resourceId,
                    String ipAddress, String details) {
        AuditLog auditLog = AuditLog.builder()
                .userEmail(userEmail)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log saved: action={}, user={}, resourceType={}, resourceId={}",
                action, userEmail, resourceType, resourceId);
    }
}
