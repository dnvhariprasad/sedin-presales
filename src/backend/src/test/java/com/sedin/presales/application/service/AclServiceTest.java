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
import com.sedin.presales.domain.enums.UserStatus;
import com.sedin.presales.domain.repository.AclEntryRepository;
import com.sedin.presales.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AclServiceTest {

    @Mock
    private AclEntryRepository aclEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionEvaluator permissionEvaluator;

    @InjectMocks
    private AclService aclService;

    @Test
    @DisplayName("grantAccess should create ACL entry")
    void grantAccess_shouldCreateAclEntry() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        User user = buildUser(userId, "user@test.com", "Test User", Role.EDITOR);

        GrantAccessRequest request = GrantAccessRequest.builder()
                .userId(userId)
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(resourceId)
                .permission(Permission.READ)
                .build();

        AclEntry savedEntry = AclEntry.builder()
                .id(entryId)
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(resourceId)
                .user(user)
                .permission(Permission.READ)
                .grantedBy("admin@test.com")
                .createdAt(Instant.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aclEntryRepository.existsByResourceTypeAndResourceIdAndUserIdAndPermission(
                ResourceType.DOCUMENT, resourceId, userId, Permission.READ)).thenReturn(false);
        when(aclEntryRepository.save(any(AclEntry.class))).thenReturn(savedEntry);

        AclEntryDto result = aclService.grantAccess(request, "admin@test.com");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(entryId);
        assertThat(result.getPermission()).isEqualTo(Permission.READ);
        assertThat(result.getUserEmail()).isEqualTo("user@test.com");
        verify(aclEntryRepository).save(any(AclEntry.class));
    }

    @Test
    @DisplayName("grantAccess should throw ResourceNotFoundException when user not found")
    void grantAccess_shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        GrantAccessRequest request = GrantAccessRequest.builder()
                .userId(userId)
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(UUID.randomUUID())
                .permission(Permission.READ)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aclService.grantAccess(request, "admin@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("grantAccess should throw DuplicateResourceException when entry already exists")
    void grantAccess_shouldThrowWhenDuplicate() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User user = buildUser(userId, "user@test.com", "Test User", Role.EDITOR);

        GrantAccessRequest request = GrantAccessRequest.builder()
                .userId(userId)
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(resourceId)
                .permission(Permission.READ)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aclEntryRepository.existsByResourceTypeAndResourceIdAndUserIdAndPermission(
                ResourceType.DOCUMENT, resourceId, userId, Permission.READ)).thenReturn(true);

        assertThatThrownBy(() -> aclService.grantAccess(request, "admin@test.com"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("bulkGrantAccess should grant access to multiple users")
    void bulkGrantAccess_shouldGrantToMultipleUsers() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        User user1 = buildUser(userId1, "user1@test.com", "User 1", Role.EDITOR);
        User user2 = buildUser(userId2, "user2@test.com", "User 2", Role.VIEWER);

        BulkGrantAccessRequest request = BulkGrantAccessRequest.builder()
                .userIds(List.of(userId1, userId2))
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(resourceId)
                .permission(Permission.READ)
                .build();

        AclEntry entry1 = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId, user1, Permission.READ);
        AclEntry entry2 = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId, user2, Permission.READ);

        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(aclEntryRepository.existsByResourceTypeAndResourceIdAndUserIdAndPermission(
                any(), any(), any(), any())).thenReturn(false);
        when(aclEntryRepository.save(any(AclEntry.class))).thenReturn(entry1, entry2);

        List<AclEntryDto> result = aclService.bulkGrantAccess(request, "admin@test.com");

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("revokeAccess should delete ACL entry")
    void revokeAccess_shouldDeleteEntry() {
        UUID entryId = UUID.randomUUID();
        when(aclEntryRepository.existsById(entryId)).thenReturn(true);

        aclService.revokeAccess(entryId);

        verify(aclEntryRepository).deleteById(entryId);
    }

    @Test
    @DisplayName("revokeAccess should throw ResourceNotFoundException when entry not found")
    void revokeAccess_shouldThrowWhenNotFound() {
        UUID entryId = UUID.randomUUID();
        when(aclEntryRepository.existsById(entryId)).thenReturn(false);

        assertThatThrownBy(() -> aclService.revokeAccess(entryId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("revokeAllAccess should delete all entries for resource")
    void revokeAllAccess_shouldDeleteAllEntries() {
        UUID resourceId = UUID.randomUUID();
        User user = buildUser(UUID.randomUUID(), "user@test.com", "User", Role.EDITOR);
        AclEntry entry = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId, user, Permission.READ);

        when(aclEntryRepository.findByResourceTypeAndResourceId(ResourceType.DOCUMENT, resourceId))
                .thenReturn(List.of(entry));

        aclService.revokeAllAccess(ResourceType.DOCUMENT, resourceId);

        verify(aclEntryRepository).deleteAll(List.of(entry));
    }

    @Test
    @DisplayName("getAccessList should return entries for resource")
    void getAccessList_shouldReturnEntries() {
        UUID resourceId = UUID.randomUUID();
        User user = buildUser(UUID.randomUUID(), "user@test.com", "User", Role.EDITOR);
        AclEntry entry = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId, user, Permission.READ);

        when(aclEntryRepository.findByResourceTypeAndResourceId(ResourceType.DOCUMENT, resourceId))
                .thenReturn(List.of(entry));

        List<AclEntryDto> result = aclService.getAccessList(ResourceType.DOCUMENT, resourceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResourceId()).isEqualTo(resourceId);
        assertThat(result.get(0).getPermission()).isEqualTo(Permission.READ);
    }

    @Test
    @DisplayName("getUserPermissions should return user entries for resource")
    void getUserPermissions_shouldReturnUserEntries() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User user = buildUser(userId, "user@test.com", "User", Role.EDITOR);
        AclEntry entry = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId, user, Permission.WRITE);

        when(aclEntryRepository.findByResourceTypeAndResourceIdAndUserId(ResourceType.DOCUMENT, resourceId, userId))
                .thenReturn(List.of(entry));

        List<AclEntryDto> result = aclService.getUserPermissions(userId, ResourceType.DOCUMENT, resourceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPermission()).isEqualTo(Permission.WRITE);
    }

    @Test
    @DisplayName("hasPermission should return true for admin user")
    void hasPermission_shouldReturnTrueForAdmin() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User adminUser = buildUser(userId, "admin@test.com", "Admin", Role.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));

        boolean result = aclService.hasPermission(userId, ResourceType.DOCUMENT, resourceId, Permission.WRITE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasPermission should return true when user has sufficient permission")
    void hasPermission_shouldReturnTrueWhenSufficientPermission() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User user = buildUser(userId, "user@test.com", "User", Role.EDITOR);
        AclEntry entry = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId, user, Permission.WRITE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aclEntryRepository.findByResourceTypeAndResourceIdAndUserId(ResourceType.DOCUMENT, resourceId, userId))
                .thenReturn(List.of(entry));
        when(permissionEvaluator.isPermissionSufficient(Permission.WRITE, Permission.READ)).thenReturn(true);

        boolean result = aclService.hasPermission(userId, ResourceType.DOCUMENT, resourceId, Permission.READ);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasPermission should return false when user has insufficient permission")
    void hasPermission_shouldReturnFalseWhenInsufficientPermission() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User user = buildUser(userId, "user@test.com", "User", Role.VIEWER);
        AclEntry entry = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId, user, Permission.READ);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aclEntryRepository.findByResourceTypeAndResourceIdAndUserId(ResourceType.DOCUMENT, resourceId, userId))
                .thenReturn(List.of(entry));
        when(permissionEvaluator.isPermissionSufficient(Permission.READ, Permission.WRITE)).thenReturn(false);

        boolean result = aclService.hasPermission(userId, ResourceType.DOCUMENT, resourceId, Permission.WRITE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getAccessibleResourceIds should return filtered resource IDs")
    void getAccessibleResourceIds_shouldReturnFilteredIds() {
        UUID userId = UUID.randomUUID();
        UUID resourceId1 = UUID.randomUUID();
        UUID resourceId2 = UUID.randomUUID();

        User user = buildUser(userId, "user@test.com", "User", Role.EDITOR);

        AclEntry entry1 = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId1, user, Permission.WRITE);
        AclEntry entry2 = buildAclEntry(UUID.randomUUID(), ResourceType.DOCUMENT, resourceId2, user, Permission.READ);
        AclEntry entry3 = buildAclEntry(UUID.randomUUID(), ResourceType.FOLDER, UUID.randomUUID(), user, Permission.WRITE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aclEntryRepository.findByUserId(userId)).thenReturn(List.of(entry1, entry2, entry3));
        when(permissionEvaluator.isPermissionSufficient(Permission.WRITE, Permission.WRITE)).thenReturn(true);
        when(permissionEvaluator.isPermissionSufficient(Permission.READ, Permission.WRITE)).thenReturn(false);

        Set<UUID> result = aclService.getAccessibleResourceIds(userId, ResourceType.DOCUMENT, Permission.WRITE);

        assertThat(result).hasSize(1);
        assertThat(result).contains(resourceId1);
    }

    private User buildUser(UUID id, String email, String displayName, Role role) {
        User user = User.builder()
                .email(email)
                .displayName(displayName)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(id);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    private AclEntry buildAclEntry(UUID id, ResourceType resourceType, UUID resourceId,
                                   User user, Permission permission) {
        return AclEntry.builder()
                .id(id)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .user(user)
                .permission(permission)
                .grantedBy("admin@test.com")
                .createdAt(Instant.now())
                .build();
    }
}
