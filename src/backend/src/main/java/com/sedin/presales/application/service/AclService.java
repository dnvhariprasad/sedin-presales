package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.AclEntryDto;
import com.sedin.presales.application.dto.BulkGrantAccessRequest;
import com.sedin.presales.application.dto.GrantAccessRequest;
import com.sedin.presales.application.exception.DuplicateResourceException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.AclEntry;
import com.sedin.presales.domain.entity.User;
import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import com.sedin.presales.domain.enums.Role;
import com.sedin.presales.domain.repository.AclEntryRepository;
import com.sedin.presales.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AclService {

    private final AclEntryRepository aclEntryRepository;
    private final UserRepository userRepository;
    private final PermissionEvaluator permissionEvaluator;

    public AclService(AclEntryRepository aclEntryRepository,
                      UserRepository userRepository,
                      PermissionEvaluator permissionEvaluator) {
        this.aclEntryRepository = aclEntryRepository;
        this.userRepository = userRepository;
        this.permissionEvaluator = permissionEvaluator;
    }

    @Transactional
    public AclEntryDto grantAccess(GrantAccessRequest request, String grantedBy) {
        log.info("Granting {} access on {}:{} to user {}",
                request.getPermission(), request.getResourceType(), request.getResourceId(), request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        boolean exists = aclEntryRepository.existsByResourceTypeAndResourceIdAndUserIdAndPermission(
                request.getResourceType(), request.getResourceId(), request.getUserId(), request.getPermission());

        if (exists) {
            throw new DuplicateResourceException(
                    String.format("ACL entry already exists for user %s on %s:%s with permission %s",
                            request.getUserId(), request.getResourceType(), request.getResourceId(), request.getPermission()));
        }

        AclEntry aclEntry = AclEntry.builder()
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .user(user)
                .permission(request.getPermission())
                .grantedBy(grantedBy)
                .build();

        AclEntry saved = aclEntryRepository.save(aclEntry);
        log.info("Granted access with ACL entry id: {}", saved.getId());

        return toDto(saved);
    }

    @Transactional
    public List<AclEntryDto> bulkGrantAccess(BulkGrantAccessRequest request, String grantedBy) {
        log.info("Bulk granting {} access on {}:{} to {} users",
                request.getPermission(), request.getResourceType(), request.getResourceId(), request.getUserIds().size());

        return request.getUserIds().stream()
                .map(userId -> {
                    GrantAccessRequest grantRequest = GrantAccessRequest.builder()
                            .userId(userId)
                            .resourceType(request.getResourceType())
                            .resourceId(request.getResourceId())
                            .permission(request.getPermission())
                            .build();
                    return grantAccess(grantRequest, grantedBy);
                })
                .toList();
    }

    @Transactional
    public void revokeAccess(UUID aclEntryId) {
        log.info("Revoking ACL entry with id: {}", aclEntryId);

        if (!aclEntryRepository.existsById(aclEntryId)) {
            throw new ResourceNotFoundException("AclEntry", "id", aclEntryId);
        }

        aclEntryRepository.deleteById(aclEntryId);
        log.info("Revoked ACL entry with id: {}", aclEntryId);
    }

    @Transactional
    public void revokeAllAccess(ResourceType resourceType, UUID resourceId) {
        log.info("Revoking all access for {}:{}", resourceType, resourceId);

        List<AclEntry> entries = aclEntryRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
        aclEntryRepository.deleteAll(entries);

        log.info("Revoked {} ACL entries for {}:{}", entries.size(), resourceType, resourceId);
    }

    @Transactional(readOnly = true)
    public List<AclEntryDto> getAccessList(ResourceType resourceType, UUID resourceId) {
        log.debug("Getting access list for {}:{}", resourceType, resourceId);

        return aclEntryRepository.findByResourceTypeAndResourceId(resourceType, resourceId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AclEntryDto> getUserPermissions(UUID userId, ResourceType resourceType, UUID resourceId) {
        log.debug("Getting permissions for user {} on {}:{}", userId, resourceType, resourceId);

        return aclEntryRepository.findByResourceTypeAndResourceIdAndUserId(resourceType, resourceId, userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, ResourceType resourceType, UUID resourceId, Permission requiredPermission) {
        log.debug("Checking if user {} has {} permission on {}:{}", userId, requiredPermission, resourceType, resourceId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ADMIN) {
            log.debug("User {} is ADMIN, granting access", userId);
            return true;
        }

        List<AclEntry> entries = aclEntryRepository.findByResourceTypeAndResourceIdAndUserId(
                resourceType, resourceId, userId);

        return entries.stream()
                .anyMatch(entry -> permissionEvaluator.isPermissionSufficient(entry.getPermission(), requiredPermission));
    }

    @Transactional(readOnly = true)
    public Set<UUID> getAccessibleResourceIds(UUID userId, ResourceType resourceType, Permission minPermission) {
        log.debug("Getting accessible resource IDs for user {} with min permission {} on type {}",
                userId, minPermission, resourceType);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ADMIN) {
            log.debug("User {} is ADMIN, returning all resources is not supported here; returning entries from ACL", userId);
        }

        return aclEntryRepository.findByUserId(userId).stream()
                .filter(entry -> entry.getResourceType() == resourceType)
                .filter(entry -> permissionEvaluator.isPermissionSufficient(entry.getPermission(), minPermission))
                .map(AclEntry::getResourceId)
                .collect(Collectors.toSet());
    }

    private AclEntryDto toDto(AclEntry aclEntry) {
        User user = aclEntry.getUser();
        return AclEntryDto.builder()
                .id(aclEntry.getId())
                .resourceType(aclEntry.getResourceType())
                .resourceId(aclEntry.getResourceId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userDisplayName(user.getDisplayName())
                .permission(aclEntry.getPermission())
                .grantedBy(aclEntry.getGrantedBy())
                .grantedAt(aclEntry.getCreatedAt())
                .build();
    }
}
