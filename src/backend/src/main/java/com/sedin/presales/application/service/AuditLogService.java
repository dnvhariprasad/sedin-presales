package com.sedin.presales.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.AuditLogDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.AuditLog;
import com.sedin.presales.domain.repository.AuditLogRepository;
import com.sedin.presales.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
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

        if (userEmail != null && !"anonymous".equals(userEmail)) {
            userRepository.findByEmail(userEmail).ifPresent(auditLog::setUser);
        }

        auditLogRepository.save(auditLog);
        log.debug("Audit log saved: action={}, user={}, resourceType={}, resourceId={}",
                action, userEmail, resourceType, resourceId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> list(Pageable pageable, String userEmail, String action,
                                           String resourceType, Instant fromDate, Instant toDate) {
        log.debug("Listing audit logs with userEmail={}, action={}, resourceType={}, fromDate={}, toDate={}",
                userEmail, action, resourceType, fromDate, toDate);

        Specification<AuditLog> spec = Specification.where(null);

        if (userEmail != null && !userEmail.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userEmail"), userEmail));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (resourceType != null && !resourceType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceType"), resourceType));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }

        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);

        return PagedResponse.<AuditLogDto>builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AuditLogDto getById(UUID id) {
        log.debug("Getting audit log by id: {}", id);
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));
        return toDto(auditLog);
    }

    private AuditLogDto toDto(AuditLog auditLog) {
        Object parsedDetails = null;
        if (auditLog.getDetails() != null) {
            try {
                parsedDetails = objectMapper.readValue(auditLog.getDetails(), Object.class);
            } catch (Exception e) {
                log.warn("Failed to parse audit log details as JSON, using raw string: {}", e.getMessage());
                parsedDetails = auditLog.getDetails();
            }
        }

        return AuditLogDto.builder()
                .id(auditLog.getId())
                .userEmail(auditLog.getUserEmail())
                .action(auditLog.getAction())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .details(parsedDetails)
                .ipAddress(auditLog.getIpAddress())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
