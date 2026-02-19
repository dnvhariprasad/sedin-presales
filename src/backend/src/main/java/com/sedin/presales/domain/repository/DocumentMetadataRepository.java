package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, UUID> {

    Optional<DocumentMetadata> findByDocumentId(UUID documentId);
}
