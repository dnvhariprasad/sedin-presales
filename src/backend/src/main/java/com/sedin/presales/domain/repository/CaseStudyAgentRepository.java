package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.CaseStudyAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseStudyAgentRepository extends JpaRepository<CaseStudyAgent, UUID> {

    List<CaseStudyAgent> findByIsActiveTrue();

    Optional<CaseStudyAgent> findFirstByIsActiveTrue();
}
