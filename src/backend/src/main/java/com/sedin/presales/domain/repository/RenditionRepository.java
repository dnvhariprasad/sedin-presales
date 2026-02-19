package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.Rendition;
import com.sedin.presales.domain.enums.RenditionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RenditionRepository extends JpaRepository<Rendition, UUID> {

    List<Rendition> findByDocumentVersionId(UUID documentVersionId);

    Optional<Rendition> findByDocumentVersionIdAndRenditionType(UUID documentVersionId, RenditionType renditionType);
}
