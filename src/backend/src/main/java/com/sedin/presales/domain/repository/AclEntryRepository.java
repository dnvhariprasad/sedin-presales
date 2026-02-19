package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.AclEntry;
import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AclEntryRepository extends JpaRepository<AclEntry, UUID> {

    List<AclEntry> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    List<AclEntry> findByUserId(UUID userId);

    List<AclEntry> findByResourceTypeAndResourceIdAndUserId(ResourceType resourceType, UUID resourceId, UUID userId);

    boolean existsByResourceTypeAndResourceIdAndUserIdAndPermission(
            ResourceType resourceType, UUID resourceId, UUID userId, Permission permission);
}
