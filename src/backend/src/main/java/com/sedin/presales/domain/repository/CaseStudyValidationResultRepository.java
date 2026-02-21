package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.CaseStudyValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseStudyValidationResultRepository extends JpaRepository<CaseStudyValidationResult, UUID> {
    Optional<CaseStudyValidationResult> findTopByDocumentVersionIdOrderByCreatedAtDesc(UUID documentVersionId);
}
