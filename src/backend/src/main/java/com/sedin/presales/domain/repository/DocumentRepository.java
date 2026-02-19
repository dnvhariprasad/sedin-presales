package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {

    List<Document> findByFolderIdAndStatus(UUID folderId, DocumentStatus status);

    List<Document> findByStatus(DocumentStatus status);
}
